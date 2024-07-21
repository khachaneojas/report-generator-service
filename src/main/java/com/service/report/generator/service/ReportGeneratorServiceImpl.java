package com.service.report.generator.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.service.report.generator.dto.*;
import com.service.report.generator.dto.payload.LoginRequest;
import com.service.report.generator.entity.*;
import com.service.report.generator.exception.BadCredentialsException;
import com.service.report.generator.repository.*;
import com.service.report.generator.exception.InvalidDataException;
import com.service.report.generator.tag.*;
import com.service.report.generator.utility.FileUtils;
import com.service.report.generator.utility.JsonConverter;
import com.service.report.generator.utility.JwtWizard;
import com.service.report.generator.utility.TextHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGeneratorServiceImpl implements ReportGeneratorService{

    private final FileUtils fileUtils;
    private final TextHelper textHelper;
    private final FileDataRepository fileDataRepository;
    private final JobScheduleTimingRepository jobScheduleTimingRepository;
    private final JsonConverter jsonConverter;
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtWizard jwtUtils;
    private final UserRoleRepository userRoleRepository;
    private final TransformationRuleRepository transformationRuleRepository;
    private final RegistryRepository registryRepository;

    private static final String ERROR_GENERIC_MESSAGE = "Oops! Something went wrong.";
    private static final String JOB_NAME = "Report Generation";
    private static final String JOB_DESCRIPTION = "This job is intended to generate a report using certain transformation rules for the specified files";
    private static final long MAX_TOTAL_FILES_SIZE = 3072L * 1024L * 1024L;
    private final AtomicBoolean isJobProcessing = new AtomicBoolean(false);

    @Value("${app.upload.dir.doc}")
    private String documentDirectory;

    @Value("${app.upload.dir.out}")
    private String outputDirectory;


    /**
     * Initializes the system upon bean construction. Ensures the existence of the document directory.
     * This method is annotated with @PostConstruct to indicate that it should be executed after the bean is constructed.
     * It also applies transactional behavior with SERIALIZABLE isolation level and REQUIRED propagation.
     */
    @PostConstruct
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public void init() {

        // Ensure the existence of the document directory.
        fileUtils.ensureDirectoryExists(documentDirectory);
        fileUtils.ensureDirectoryExists(outputDirectory);


        // Ensure the existense of required schedule time for scheduling a report generation job
        if(0 == jobScheduleTimingRepository.count()){
            jobScheduleTimingRepository.save(
                    JobScheduleTimingModel.builder()
                            .jobType(JobType.REPORT_GENERATOR)
                            .scheduleTime(LocalTime.of(16, 15)) // 18:00 IST / 12:30 as per UTC in db
                            .build()
            );
        }

        if(0 == userRoleRepository.count()){

            List<UserRole> userRoles = List.of(UserRole.DEFAULT, UserRole.ADMIN);

            List<UserRoleModel> userRoleModels = userRoles.stream()
                    .map(role -> UserRoleModel.builder()
                            .role(role)
                            .build()
                    ).collect(Collectors.toCollection(ArrayList::new));

            userRoleRepository.saveAll(userRoleModels);
        }

        if(0 == userRepository.count()){
            userRepository.save(
                    UserModel.builder()
                            .userUid(generateUserUid())
                            .email("default@gmail.com")
                            .password(passwordEncoder.encode("Default@123"))
                            .tokenAt(Instant.now())
                            .userRoles(List.of(userRoleRepository.findById(2L).get()))
                            .build()
            );
        }

        if(0 == transformationRuleRepository.count()) {

            Map<Integer, RuleDTO> ruleForField1Map = new LinkedHashMap<>(){{
            put(0,RuleDTO.builder().id(1).col(0).build());
            put(1,RuleDTO.builder().id(1).col(1).build());
            }};

            Map<Integer, RuleDTO> ruleForField2Map = new LinkedHashMap<>(){{
                put(0,RuleDTO.builder().id(1).col(2).build());
            }};

            Map<Integer, RuleDTO> ruleForField3Map = new LinkedHashMap<>(){{
                put(0,RuleDTO.builder().id(2).col(0).build());
                put(1,RuleDTO.builder().id(2).col(1).build());
                put(2,RuleDTO.builder().id(1).col(3).build());
            }};

            Map<Integer, RuleDTO> ruleForField4Map = new LinkedHashMap<>(){{
                put(0,RuleDTO.builder().id(2).col(3).build());
                put(1,RuleDTO.builder().id(2).col(2).build());
                put(2,RuleDTO.builder().id(2).col(2).build());
            }};

            Map<Integer, RuleDTO> ruleForField5Map = new LinkedHashMap<>(){{
                put(0,RuleDTO.builder().id(3).col(0).build());
            }};

            Map<Integer, RuleDTO> ruleForField6Map = new LinkedHashMap<>(){{
                put(0,RuleDTO.builder().id(3).col(1).build());
                put(1,RuleDTO.builder().id(3).col(0).build());
            }};

            Map<Integer, RuleDTO> ruleForField7Map = new LinkedHashMap<>(){{
                put(0,RuleDTO.builder().id(1).col(4).build());
            }};

            String field1Data, field2Data, field3Data, field4Data, field5Data, field6Data, field7Data;
            try {
                field1Data = jsonConverter.convertMapToJsonString(ruleForField1Map);
                field2Data = jsonConverter.convertMapToJsonString(ruleForField2Map);
                field3Data = jsonConverter.convertMapToJsonString(ruleForField3Map);
                field4Data = jsonConverter.convertMapToJsonString(ruleForField4Map);
                field5Data = jsonConverter.convertMapToJsonString(ruleForField5Map);
                field6Data = jsonConverter.convertMapToJsonString(ruleForField6Map);
                field7Data = jsonConverter.convertMapToJsonString(ruleForField7Map);
            } catch (JsonProcessingException e) {
                throw new InvalidDataException(ERROR_GENERIC_MESSAGE);
            }

            List<TransformationRuleModel> transformationRules = List.of(
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD1)
                            .columnName("FullName")
                            .operationType(OperationType.SPACE_BETWEEN)
                            .transformationExpression("0,1")
                            .transformationData(field1Data)
                            .build(),
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD2)
                            .columnName("Address")
                            .operationType(OperationType.DEFAULT)
                            .transformationExpression("0")
                            .transformationData(field2Data)
                            .build(),
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD3)
                            .columnName("MaritalStatus")
                            .operationType(OperationType.COMMA_SEPARATED)
                            .transformationExpression("0,1,2")
                            .transformationData(field3Data)
                            .build(),
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD4)
                            .columnName("BMI")
                            .operationType(OperationType.MATHEMATICAL)
                            .transformationExpression("<>>>0<<<> / <>>>1<<<> * <>>>2<<<>")
                            .transformationData(field4Data)
                            .build(),
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD5)
                            .columnName("AnnualIncome")
                            .operationType(OperationType.MATHEMATICAL)
                            .transformationExpression("<>>>0<<<> * 12")
                            .transformationData(field5Data)
                            .build(),
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD6)
                            .columnName("TaxPaid")
                            .operationType(OperationType.MATHEMATICAL)
                            .transformationExpression("<>>>0<<<> * <>>>1<<<> * 12")
                            .transformationData(field6Data)
                            .build(),
                    TransformationRuleModel.builder()
                            .fieldName(FieldName.OUTFIELD7)
                            .columnName("NationalIdentifier")
                            .operationType(OperationType.DEFAULT)
                            .transformationExpression("0")
                            .transformationData(field7Data)
                            .build()
            );

            transformationRuleRepository.saveAll(transformationRules);
        }

        if(0 == registryRepository.count()){
            DeviceRegistryModel deviceRegistryModel = jobProcessor.getInstance();
            registryRepository.save(deviceRegistryModel);
        }

    }





    /**
     * Generates a unique user UID based on the current date and a random UUID suffix.
     * The method ensures that the generated UID does not already exist in the database.
     *
     * @return A unique user UID.
     * @throws InvalidDataException If the method fails to generate a unique UID after 100 attempts.
     */
    public String generateUserUid() {
        // Generate the prefix using the current date-time in a specific format
        String prefix = "U" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
        String userUID;
        boolean existsInDatabase;
        int attempts = 0;
        do {
            // Generate a random UUID suffix and combine it with the prefix to create the UID
            String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
            userUID = prefix + uuid;
            // Check if the generated UID already exists in the database
            existsInDatabase = userRepository.existsByUserUid(userUID);
            attempts++;
        } while (existsInDatabase && attempts <= 100);

        // If the generated UID still exists after 100 attempts, throw an exception
        if (existsInDatabase) {
            throw new InvalidDataException("Failed to execute current task.Try again");
        }

        return userUID;
    }


    /**
     * Generates a unique job UID based on the current date and a random UUID suffix.
     * The method ensures that the generated UID does not already exist in the database.
     *
     * @return A unique job UID.
     * @throws InvalidDataException If the method fails to generate a unique UID after 100 attempts.
     */
    public String generateJobUid() {
        // Generate the prefix using the current date-time in a specific format
        String prefix = "J" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String jobUID;
        boolean existsInDatabase;
        int attempts = 0;
        do {
            // Generate a random UUID suffix and combine it with the prefix to create the UID
            String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
            jobUID = prefix + uuid;
            // Check if the generated UID already exists in the database
            existsInDatabase = jobRepository.existsByJobUid(jobUID);
            attempts++;
        } while (existsInDatabase && attempts <= 100);

        // If the generated UID still exists after 100 attempts, throw an exception
        if (existsInDatabase) {
            throw new InvalidDataException("Failed to execute current task.Try again");
        }

        return jobUID;
    }



    public static String formattedNOW() {
        return Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
    }


    /**
     * Handles the upload of main and reference files, and processes them based on their types.
     * @param mainfile The main file to be uploaded, which is mandatory.
     * @param reference1 An optional reference file to be uploaded.
     * @param reference2 An optional second reference file to be uploaded.
     * @param validationResponse The validation response containing user information.
     * @return APIResponse indicating the success or failure of the file upload operation.
     * @throws InvalidDataException if the main file is null or empty, or if there is an application-level error during processing.
     */
    @Override
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public APIResponse<?> uploadFile(
            MultipartFile mainfile,
            MultipartFile reference1,
            MultipartFile reference2,
            TokenValidationResponse validationResponse
    ) {

        // Check if the main file is provided and not empty
        if(null == mainfile || mainfile.isEmpty())
            throw new InvalidDataException("To proceed, provide with a input file");

        // Retrieve the logged-in user information using the validation response
        Optional<UserModel> loggedInUser = userRepository.findById(validationResponse.getPid());

        // Determine if reference files are provided
        boolean isReference1Present = null != reference1 && !reference1.isEmpty();
        boolean isReference2Present = null != reference2 && !reference2.isEmpty();

        // Prepare a map to hold file type and corresponding MultipartFile list
        Map<FileType, List<MultipartFile>> multipartFileMap = new LinkedHashMap<>();

        // Add main file to the map
        List<MultipartFile> mainMultipartFile = new ArrayList<>();
        mainMultipartFile.add(mainfile);
        multipartFileMap.put(FileType.MAIN,mainMultipartFile);

        // Add reference files to the map if present
        List<MultipartFile> referenceMultipartFile = new ArrayList<>();
        if(isReference1Present)
            referenceMultipartFile.add(reference1);

        if(isReference2Present)
            referenceMultipartFile.add(reference2);

        if(!referenceMultipartFile.isEmpty())
            multipartFileMap.put(FileType.REFERENCE, referenceMultipartFile);

        // Update document data models from the provided files
        Map<FileType, List<FileDataDTO>> fileDataDTOMap= updateDocumentByFileDataModel(
                multipartFileMap,
                documentDirectory
        );

        // Retrieve FileDataDTO objects for the uploaded files
        FileDataDTO mainFileDTO = fileDataDTOMap.get(FileType.MAIN).get(0);

        FileDataDTO reference1DTO = null;
        FileDataDTO reference2DTO = null;

        List<FileDataDTO> referenceDTOList = fileDataDTOMap.get(FileType.REFERENCE);
        if(null != referenceDTOList && !referenceDTOList.isEmpty()){
            reference1DTO = referenceDTOList.get(0);

            if(1 < referenceDTOList.size())
                reference2DTO = referenceDTOList.get(1);
        }

        // Determine the file extension type based on the uploaded files
        FileExtensionType fileExtensionType = getFileType(mainFileDTO.getFileExtension(),
                null != reference1DTO ? reference1DTO.getFileExtension() : null,
                null != reference2DTO ? reference2DTO.getFileExtension() : null);

        switch (fileExtensionType){

            case CSV -> {
                // Prepare file ID lists for scheduling report generation
                Map<FileType, ListDTO> fileMap = new LinkedHashMap<>();

                // Adding main file ID to the map
                List<Long> mainFileIdList = new ArrayList<>();
                mainFileIdList.add(mainFileDTO.getFileDataModel().getFileId());
                fileMap.put(FileType.MAIN, ListDTO.builder().id(mainFileIdList).build());

                // Adding reference file IDs to the map
                List<Long> referenceFileIdList = new ArrayList<>();
                if (null != reference1DTO)
                    referenceFileIdList.add(reference1DTO.getFileDataModel().getFileId());

                if (null != reference2DTO)
                    referenceFileIdList.add(reference2DTO.getFileDataModel().getFileId());

                if(!referenceFileIdList.isEmpty())
                    fileMap.put(FileType.REFERENCE,  ListDTO.builder().id(referenceFileIdList).build());

                // Schedule a job for report generation based on the uploaded files
                scheduleJobForReportGeneration(fileMap, loggedInUser.get());

                // Return success message in APIResponse
                return APIResponse.builder()
                        .message("Files successfully uploaded.")
                        .build();
            }

        }

        // Return message indicating that only CSV files are supported
        return APIResponse.builder()
                .message("Currently we only serve csv files")
                .build();

    }



    /**
     * Triggers the report generation process for the specified job ID and returns the result.
     * @param jobId The unique identifier for the job to be processed.
     * @return APIResponse indicating the success of the report generation and the name of the created output file.
     * @throws InvalidDataException if the job ID is not found in the repository or if there is an application-level error during processing.
     */
    @Override
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED
    )
    public APIResponse<?> triggerReportGeneration(
            String jobId
    ) {
        // Retrieve the job model from the repository based on the provided job ID
        Optional<JobModel> jobModel = jobRepository.findByJobUid(jobId);

        // If the job model is not found, throw an exception
        if(jobModel.isEmpty())
            throw new InvalidDataException("Job not found for given ID, double-check and try again");

        // Execute the report generation process for the found job model
        String outputFileName = executeReportGeneration(jobModel.get());

        // Return success message with the name of the created output file
        return APIResponse.builder()
                .message("Transformation completed successfully. The output file ("+outputFileName+") has been created.")
                .build();
    }



    /**
     * Executes the report generation process based on the provided job model and returns the name of the created output file.
     * @param jobModel The job model containing configuration and file references for the report generation.
     * @return The name of the created output file.
     * @throws InvalidDataException if there is an issue with file processing or if job data is invalid.
     * @throws IOException if there is an error during file I/O operations.
     */
    public String executeReportGeneration(
            JobModel jobModel
    ){
        // Retrieve the JSON data from the job model and convert it to a map of file types to list DTOs
        String jobData = jobModel.getJsonData();

        Map<FileType, ListDTO> jobDataMap = jsonConverter.getMapFromJsonString(jobData, FileType.class, ListDTO.class);

        // Get the main file data model and reference file data models from the repository
        ListDTO mainListDTO = jobDataMap.get(FileType.MAIN);
        Optional<FileDataModel> mainFileDataModel = fileDataRepository.findById(mainListDTO.getId().get(0));

        Optional<FileDataModel> referenceFile1 = Optional.empty();
        Optional<FileDataModel> referenceFile2 = Optional.empty();
        ListDTO referenceListDTO =  jobDataMap.get(FileType.REFERENCE);
        if(null != referenceListDTO) {

            List<Long> referenceFileID = referenceListDTO.getId();
            referenceFile1 = fileDataRepository.findById(referenceFileID.get(0));

            if(1 < referenceFileID.size())
                referenceFile2 = fileDataRepository.findById(referenceFileID.get(1));
        }

        OutputFileDTO outputFileDTO;
        try {
            // Load reference files into maps for processing
            Map<String, CSVRecord> ref1Map = loadReferenceFile(referenceFile1.get().getFilePath(), "NationalIdentifier");
            Map<String, CSVRecord> ref2Map = loadReferenceFile(referenceFile2.get().getFilePath(), "NationalIdentifier");

            // Process the main file and generate the output
            outputFileDTO = processMainFile(
                    mainFileDataModel.get().getFilePath(),
                    ref1Map,
                    ref2Map,
                    outputDirectory
            );

        } catch (IOException e) {
            throw new InvalidDataException("File processing failed.");
        }

        DeviceRegistryModel deviceRegistryModel = jobProcessor.getInstance();

        // Update the job model status and save it in the repository
        jobModel.setStatus(JobStatus.SUCCESS);
        jobModel.setAttempts(jobModel.getAttempts() + 1);
        jobModel.setLastRanAt(Instant.now());
        jobModel.setLastRanBy(deviceRegistryModel.getMacAddress());
        jobRepository.save(jobModel);

        // Save the output file information in the repository
        String outputFileName = outputFileDTO.getOutputFileName() + ".csv";

        fileDataRepository.save(
                FileDataModel.builder()
                        .fileName(outputFileName)
                        .fileOriginal(outputFileName)
                        .filePath(outputFileDTO.getFilePath())
                        .fileType(outputFileDTO.getFileType())
                        .fileCategory(FileCategory.OUTPUT)
                        .build()
        );

        // Return the name of the created output file
        return outputFileName;
    }



    /**
     * Loads a CSV file and returns a map of CSV records indexed by a specified column value.
     * @param filePath The path to the CSV file to be loaded.
     * @param idColumnName The name of the column to be used as the key in the resulting map.
     * @return A map where the keys are values from the specified column and the values are the corresponding CSV records.
     * @throws IOException if an error occurs while reading the file.
     */
    public static Map<String, CSVRecord> loadReferenceFile(
            String filePath,
            String idColumnName
    ) throws IOException {

        Map<String, CSVRecord> map = new HashMap<>();
        // Open the file and read it using BufferedReader
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Set up the CSVFormat with header and skip header record
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader() // Indicates the first record should be used as headers
                    .setSkipHeaderRecord(true) // Skip the header record while reading data
                    .build();
            // Parse the CSV file
            try (CSVParser parser = new CSVParser(reader, format)) {
                // Iterate over each record in the CSV file
                for (CSVRecord record : parser) {
                    // Get the value from the specified column and use it as the key
                    String id = record.get(idColumnName);
                    // Add the record to the map
                    map.put(id, record);
                }
            }
        }
        // Return the populated map
        return map;
    }


    /**
     * Processes the main CSV file by applying transformation rules and generates an output CSV file.
     * @param mainFilePath The path to the main CSV file to be processed.
     * @param ref1Map A map of reference records from the first reference file, indexed by a key column value.
     * @param ref2Map A map of reference records from the second reference file, indexed by a key column value.
     * @param outputDirectory The directory where the output CSV file will be saved.
     * @return An OutputFileDTO containing the name, path, and type of the generated output file.
     * @throws InvalidDataException if there is an error during data processing.
     */
    public OutputFileDTO processMainFile(
            String mainFilePath,
            Map<String, CSVRecord> ref1Map,
            Map<String, CSVRecord> ref2Map,
            String outputDirectory
    ) throws IOException {

        // Ensure the output directory exists
        Files.createDirectories(Paths.get(outputDirectory));
        // Generate a random file name for the output file
        String outputFileName = generateRandomFileName();
        String outputFilePath = String.format("%s%s.csv", outputDirectory, outputFileName);

        // Get the content type of the output file
        String contentType = Files.probeContentType(Paths.get(outputFilePath));

        // Define the list of field names for which transformation rules are applied
        List<FieldName> fieldNameList = List.of(FieldName.OUTFIELD1, FieldName.OUTFIELD2, FieldName.OUTFIELD3, FieldName.OUTFIELD4, FieldName.OUTFIELD5, FieldName.OUTFIELD6, FieldName.OUTFIELD7);
        // Retrieve transformation rules for the specified field names
        List<TransformationRuleModel> transformationRules = transformationRuleRepository.findByFieldNameIn(fieldNameList);

        Map<FieldName, TransformationRuleModel> transformationRuleMap = transformationRules.stream()
                .collect(Collectors.toMap(TransformationRuleModel::getFieldName, rule -> rule));

        try (BufferedReader mainReader = new BufferedReader(new FileReader(mainFilePath))) {

            // Set up CSV format for reading the main file
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()                                // Indicates the first record should be used as headers
                    .setSkipHeaderRecord(true)                  // Skip the header record while reading data
                    .build();

            try (
                    CSVParser mainParser = new CSVParser(mainReader, format);
                    CSVPrinter outputPrinter = new CSVPrinter(new FileWriter(outputFilePath),
                            // Set headers for the output file based on transformation rules
                            CSVFormat.DEFAULT.builder()
                                 .setHeader(transformationRules.stream().map(TransformationRuleModel::getColumnName).toArray(String[]::new))     // Convert List to array
                                 .build()
                    )
            ) {

                // Process each record in the main file
                for (CSVRecord mainRecord : mainParser) {
                    // Retrieve corresponding reference records
                    String id = mainRecord.get(4);
                    CSVRecord ref1Record = ref1Map.get(id);
                    CSVRecord ref2Record = ref2Map.get(id);

                    Object[] columnData;
                    try {
                        // Process column data based on transformation rules
                        columnData = processColumnData(transformationRuleMap, mainRecord, ref1Record, ref2Record);
                    }
                    catch (Exception e){
                        // Handle errors by closing the printer and deleting the output file
                        outputPrinter.close();
                        deleteLocallySavedFiles(List.of(outputFilePath));
                        throw new InvalidDataException(ERROR_GENERIC_MESSAGE);
                    }

                    // Write processed data to the output file
                    outputPrinter.printRecord(columnData);
                }
            }

        }
        // Return details of the generated output file
        return OutputFileDTO.builder()
                .outputFileName(outputFileName)
                .filePath(outputFilePath)
                .fileType(contentType)
                .build();
    }




    /**
     * Processes and transforms column data for a given record based on transformation rules.
     * @param transformationRuleMap A map of transformation rules keyed by field names.
     * @param mainRecord The main CSV record from which data is to be transformed.
     * @param ref1Record The first reference CSV record used for transformation.
     * @param ref2Record The second reference CSV record used for transformation.
     * @return An array of transformed data for each field specified in the transformation rules.
     */
    public Object[] processColumnData(
            Map<FieldName, TransformationRuleModel> transformationRuleMap,
            CSVRecord mainRecord,
            CSVRecord ref1Record,
            CSVRecord ref2Record
    ){
        // Define the list of field names for which data needs to be transformed
        List<FieldName> fieldNameList = List.of(FieldName.OUTFIELD1, FieldName.OUTFIELD2, FieldName.OUTFIELD3, FieldName.OUTFIELD4, FieldName.OUTFIELD5, FieldName.OUTFIELD6, FieldName.OUTFIELD7);

        // Initialize an array to hold the transformed data
        Object[] ouputData = new Object[7];
        // Process each field name and apply the corresponding transformation rule
        for(int i = 0; i < ouputData.length; i++){
            FieldName fieldName = fieldNameList.get(i);
            TransformationRuleModel transformationRule = transformationRuleMap.get(fieldName);
            // Transform the data for the current field and store it in the output array
            ouputData[i] = transformColumnData(transformationRule, mainRecord, ref1Record, ref2Record);
        }

        return ouputData;
    }


    /**
     * Transforms column data based on the specified transformation rule.
     * @param transformationRule The rule that defines how to transform the data.
     * @param mainRecord The main CSV record used for data extraction.
     * @param ref1Record The first reference CSV record used for data extraction.
     * @param ref2Record The second reference CSV record used for data extraction.
     * @return The transformed data based on the operation type defined in the transformation rule.
     */
    public Object transformColumnData(
            TransformationRuleModel transformationRule,
            CSVRecord mainRecord,
            CSVRecord ref1Record,
            CSVRecord ref2Record
    ){
        // Retrieve operation type and transformation details from the rule
        OperationType operationType = transformationRule.getOperationType();
        String transformationExpression = transformationRule.getTransformationExpression();
        String transformationData = transformationRule.getTransformationData();

        // Convert the transformation data from JSON string to a map of RuleDTO objects
        Map<Integer, RuleDTO> ruleDTOMap = jsonConverter.getMapFromJsonString(transformationData, Integer.class, RuleDTO.class);

        Object outputData = null;

        // Perform transformation based on the operation type
        switch (operationType){

            case DEFAULT -> {
                // Extract column indices from the transformation expression
                List<Integer> mapIndex = extractNumbers(transformationExpression);

                RuleDTO ruleDTO = ruleDTOMap.get(mapIndex.get(0));
                Integer id = ruleDTO.getId();
                Integer columnIndex = ruleDTO.getCol();

                // Retrieve and set the data from the appropriate record based on ID
                outputData = switch (id) {
                    case 1 -> mainRecord.get(columnIndex);
                    case 2 -> null != ref1Record ? ref1Record.get(columnIndex) : null;
                    case 3 -> null != ref2Record ? ref2Record.get(columnIndex) : null;
                    default -> null;
                };
            }

            case SPACE_BETWEEN -> {
                // Extract column indices from the transformation expression
                List<Integer> mapIndex = extractNumbers(transformationExpression);

                // Collect column values and join them with a space
                List<String> columnValues = mapIndex.stream()
                        .map(index -> {

                            RuleDTO ruleDTO = ruleDTOMap.get(index);
                            Integer id = ruleDTO.getId();
                            Integer columnIndex = ruleDTO.getCol();

                            return switch (id) {
                                case 1 -> mainRecord.get(columnIndex);
                                case 2 -> null != ref1Record ? ref1Record.get(columnIndex) : null;
                                case 3 -> null != ref2Record ? ref2Record.get(columnIndex) : null;
                                default -> null;
                            };

                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(ArrayList::new));

                outputData = String.join(" ", columnValues);
            }

            case COMMA_SEPARATED -> {
                // Extract column indices from the transformation expression
                List<Integer> mapIndex = extractNumbers(transformationExpression);

                // Collect column values and join them with a comma
                List<String> columnValues = mapIndex.stream()
                        .map(index -> {

                            RuleDTO ruleDTO = ruleDTOMap.get(index);
                            Integer id = ruleDTO.getId();
                            Integer columnIndex = ruleDTO.getCol();

                            return switch (id) {
                                case 1 -> mainRecord.get(columnIndex);
                                case 2 -> null != ref1Record ? ref1Record.get(columnIndex) : null;
                                case 3 -> null != ref2Record ? ref2Record.get(columnIndex) : null;
                                default -> null;
                            };

                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(ArrayList::new));

                outputData = String.join(", ", columnValues);
            }

            case MATHEMATICAL -> {
                // Extract column indices from the transformation expression
                List<Integer> mapIndex = extractNumbersByPattern(transformationExpression);

                List<Object> columnValues = new ArrayList<>();

                for(Integer index : mapIndex) {

                    RuleDTO ruleDTO = ruleDTOMap.get(index);
                    Integer id = ruleDTO.getId();
                    Integer columnIndex = ruleDTO.getCol();

                    // Retrieve and add column values for mathematical operations
                    Object columnValue = switch (id) {
                                case 1 -> mainRecord.get(columnIndex);
                                case 2 -> null != ref1Record ? ref1Record.get(columnIndex) : null;
                                case 3 -> null != ref2Record ? ref2Record.get(columnIndex) : null;
                                default -> null;
                            };

                    if(null == columnValue)
                        break;

                    columnValues.add(columnValue);
                }

                if(!columnValues.isEmpty()){
                    // Replace placeholders in the expression and evaluate it
                    String replacedExpression = replacePlaceholders(transformationExpression, columnValues);
                    outputData = textHelper.evaluateExpression(replacedExpression);
                }

            }

        }

        return outputData;
    }


    /**
     * Replaces placeholders in the given expression with corresponding values from the list.
     * Placeholders in the format `<>>>N<<<>` are replaced, where `N` is the index of the value to be used.
     *
     * @param expression The string expression containing placeholders to be replaced.
     * @param values The list of values to replace the placeholders with. The index of the placeholder corresponds to the index in this list.
     * @return The expression with placeholders replaced by the corresponding values.
     */
    public static String replacePlaceholders(
            String expression,
            List<Object> values
    ) {
        // Define the pattern for placeholders in the format <>>>N<<<>
        String patternString = "<>>>\\d+<<<>";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(expression);

        StringBuilder result = new StringBuilder(expression);
        int offset = 0; // Offset to adjust for changes in the length of the string during replacements

        // Find all placeholders in the expression
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Extract the placeholder index
            String placeholder = expression.substring(start, end);
            String indexString = placeholder.replaceAll("[^\\d]", "");
            int index = Integer.parseInt(indexString);

            // Replace with value from the list
            String replacement = values.get(index).toString();
            result.replace(start - offset, end - offset, replacement);
            offset += (end - start) - replacement.length();
        }

        return result.toString();
    }



    /**
     * Extracts numbers from a string expression that are enclosed in the format `<>>>(N)<<<>`.
     *
     * @param expression The string expression containing numbers enclosed in the format `<>>>(N)<<<>`.
     * @return A list of integers extracted from the expression.
     */
    public static List<Integer> extractNumbersByPattern(
            String expression
    ) {

        List<Integer> numbers = new ArrayList<>();

        // Regular expression to find numbers enclosed in <>>> and <>>>
        Pattern pattern = Pattern.compile("<>>>(\\d+)<<<>");
        Matcher matcher = pattern.matcher(expression);

        // Find and extract numbers
        while (matcher.find()) {
            // Extract and convert the number to Integer, then add to the list
            numbers.add(Integer.parseInt(matcher.group(1)));
        }

        return numbers;
    }



    /**
     * Extracts integers from a comma-separated string expression.
     *
     * @param expression The string expression containing numbers separated by commas.
     * @return A list of integers extracted from the expression.
     */
    public static List<Integer> extractNumbers(
            String expression
    ) {
        List<Integer> numbers = new ArrayList<>();
        // Split the expression by commas
        String[] parts = expression.split(",");

        for (String part : parts) {
            // Trim whitespace and convert to Integer
            numbers.add(Integer.parseInt(part.trim()));
        }

        return numbers;
    }



    /**
     * Generates a random file name using a combination of a UUID and the current date.
     *
     * The file name is constructed as follows:
     * - A shortened UUID (first 4 characters) without hyphens.
     * - The current date formatted as "yyMMdd".
     *
     * @return A string representing the randomly generated file name.
     */
    public static String generateRandomFileName() {
        // Generate a UUID and remove hyphens
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
        // Get the current date formatted as "yyMMdd"
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        // Concatenate UUID and date to form the file name
        return uuid + suffix;
    }



    /**
     * Signs in a user by validating their email and password, and issues a JWT token if authentication is successful.
     *
     * @param loginRequest Contains the user's email and password for authentication.
     * @return A JwtTokenResponse containing the JWT token if authentication is successful.
     * @throws BadCredentialsException if the email is not found or the password does not match the stored password.
     */
    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRED
    )
    public JwtTokenResponse signInUser(
            LoginRequest loginRequest
    ) {
        // Sanitize input parameters to prevent potential security issues
        String email = textHelper.sanitize(loginRequest.getEmail());
        String password = textHelper.sanitize(loginRequest.getPassword());

        // Retrieve the user model from the repository based on the sanitized email
        UserModel userModel = userRepository.findByEmail(email);
        // Verify if the user exists and if the provided password matches the stored password
        if (null == userModel || !passwordEncoder.matches(password, userModel.getPassword()))
            throw new BadCredentialsException();

        // Generate a JWT token for the authenticated user
        return JwtTokenResponse.builder()
                .token(
                        jwtUtils.issueToken(
                                userModel.getUserUid(),
                                userModel.getTokenAt().toEpochMilli()
                        )
                )
                .build();
    }



    /**
     * Schedules a job for report generation by creating a JobModel and saving it to the repository.
     *
     * @param fileMap A map of FileType to ListDTO containing the file information required for report generation.
     * @param userModel The user who initiated the job scheduling.
     * @throws InvalidDataException if there is an error in converting fileMap to JSON.
     */
    public void scheduleJobForReportGeneration(
            Map<FileType, ListDTO> fileMap,
            UserModel userModel
    ){
        // Ensure that the response is not null
        Objects.requireNonNull(fileMap);

        String jobData;
        try {
            // Convert the fileMap to JSON string
            jobData = jsonConverter.convertMapToJsonString(fileMap);
        } catch (JsonProcessingException exception) {
            // Throw an exception if there is an error in JSON processing
            throw new InvalidDataException(ERROR_GENERIC_MESSAGE);
        }

        // Retrieve the scheduling timing configuration for the job
        JobScheduleTimingModel jobScheduleTimingModel = jobScheduleTimingRepository.findByJobType(JobType.REPORT_GENERATOR);

        LocalTime localTime = jobScheduleTimingModel.getScheduleTime();

        // Calculate the next schedule instant based on the retrieved local time
        Instant scheduleInstant = getNextScheduleInstant(localTime);

        // Build a JobModel object with the necessary details
        JobModel job = JobModel.builder()
                .jobUid(generateJobUid())           // Generate a unique job identifier
                .name(JOB_NAME)                     // Set the job name
                .description(JOB_DESCRIPTION)       // Set the job description
                .attempts(0)                        // Initialize the attempt counter
                .scheduleType(ScheduleType.ONCE)    // Set the job to be scheduled only once
                .status(JobStatus.QUEUED)           // Set the initial status of the job
                .executeAt(scheduleInstant)         // Set the time at which the job should be executed
                .jobType(JobType.REPORT_GENERATOR)  // Set the job type
                .jsonData(jobData)                  // Attach the job data in JSON format
                .createdBy(userModel)               // Set the user who created the job
                .lastModifiedBy(userModel)          // Set the user who last modified the job
                .build();

        // Save the job to the repository to schedule it
        jobRepository.save(job);

    }




    /**
     * Calculates the next scheduled instant based on the provided local time.
     *
     * @param localTime The time at which the job should be scheduled.
     * @return An Instant representing the next schedule time in UTC.
     */
    public static Instant getNextScheduleInstant(LocalTime localTime) {
        // Get the current date and time
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        LocalDateTime scheduleDateTime;

        if (now.isBefore(localTime)) {
            // Schedule for today at the provided local time
            scheduleDateTime = LocalDateTime.of(today, localTime);
        } else {
            // Schedule for the next day at the provided local time
            LocalDate tomorrow = today.plusDays(1);
            scheduleDateTime = LocalDateTime.of(tomorrow, localTime);
        }

        // Convert the scheduled time to an Instant in the UTC time zone
        return scheduleDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toInstant();
    }



    public FileExtensionType getFileType(
            String mainFileExtension,
            String reference1Extension,
            String reference2Extension
    ){
        
        boolean isReference1Present = null != reference1Extension && !reference1Extension.isEmpty();
        boolean isReference2Present = null != reference2Extension && !reference2Extension.isEmpty();
        
        FileExtensionType fileExtensionType = null;
        if(isReference1Present && isReference2Present){

            if(reference1Extension.equals("csv") && reference2Extension.equals("csv") && mainFileExtension.equals("csv")){
                fileExtensionType = FileExtensionType.CSV;
            }else if(reference1Extension.equals("xlsx") && reference2Extension.equals("xlsx") && mainFileExtension.equals("xlsx")){
                fileExtensionType = FileExtensionType.EXCEL;
            } else if (reference1Extension.equals("json") && reference2Extension.equals("json") && mainFileExtension.equals("json")) {
                fileExtensionType = FileExtensionType.JSON;
            }

        } else if (isReference1Present) {

            if(reference1Extension.equals("csv") && mainFileExtension.equals("csv")){
                fileExtensionType = FileExtensionType.CSV;
            }else if(reference1Extension.equals("xlsx") && mainFileExtension.equals("xlsx")){
            fileExtensionType = FileExtensionType.EXCEL;
            } else if (reference1Extension.equals("json") && mainFileExtension.equals("json")) {
                fileExtensionType = FileExtensionType.JSON;
            }

        } else if (isReference2Present) {

            if(reference2Extension.equals("csv") && mainFileExtension.equals("csv")){
                fileExtensionType = FileExtensionType.CSV;
            }else if(reference2Extension.equals("xlsx") && mainFileExtension.equals("xlsx")){
                fileExtensionType = FileExtensionType.EXCEL;
            } else if (reference2Extension.equals("json") && mainFileExtension.equals("json")) {
                fileExtensionType = FileExtensionType.JSON;
            }

        }else{

            switch (mainFileExtension) {
                case "csv" -> fileExtensionType = FileExtensionType.CSV;
                case "xlsx" -> fileExtensionType = FileExtensionType.EXCEL;
                case "json" -> fileExtensionType = FileExtensionType.JSON;
            }

        }

        return fileExtensionType;
    }




    /**
     * Updates a document based on the provided file data model.
     *
     * @param documents map consisting the file type and file to be saved.
     * @param directory     The directory where the document will be saved.
     * @throws InvalidDataException If the document is empty or has an invalid format.
     */
    public Map<FileType, List<FileDataDTO>> updateDocumentByFileDataModel(
            Map<FileType,List<MultipartFile>> documents,
            String directory
    ) {

        List<String> locallySavedFiles = new ArrayList<>();
        Map<FileType, List<FileDataDTO>> fileDataDTOMap = new LinkedHashMap<>();

        for (FileType fileType : documents.keySet()) {

            List<MultipartFile> multipartFiles = documents.get(fileType);

            List<FileDataDTO> fileDataDTOS = new ArrayList<>();
            for (MultipartFile file : multipartFiles) {
                String fileExtension = fileUtils.hasValidFileExtension(file.getOriginalFilename());

                if (null == fileExtension || file.getSize() > MAX_TOTAL_FILES_SIZE) {
                    deleteLocallySavedFiles(locallySavedFiles);
                    throw new InvalidDataException("Invalid file found.");
                }

                FileDataModel fileDataModel = saveFileLocally(directory, file);
                FileDataModel savedFileDataModel = fileDataRepository.save(fileDataModel);

                locallySavedFiles.add(savedFileDataModel.getFilePath());

                fileDataDTOS.add(FileDataDTO.builder()
                        .fileExtension(fileExtension)
                        .fileDataModel(savedFileDataModel)
                        .build()
                );
            }

            fileDataDTOMap.put(fileType, fileDataDTOS);
        }

        return fileDataDTOMap;
    }



    public void deleteLocallySavedFiles(
            List<String> locallySavedFiles
    ){
            if (!locallySavedFiles.isEmpty()) {
                for (String filePath : locallySavedFiles) {
                    fileUtils.deleteFileLocally(filePath);
                }
            }
    }



    /**
     * Automatically adds jobs to RabbitMQ for processing at a fixed delay interval.
     * This method is scheduled to run every 60 seconds with an initial delay of 60 seconds.
     */
    @Scheduled(fixedDelay = 90_000, initialDelay = 60_000)
    public void autoPublishJobsInQueue() {
        if (isJobProcessing.compareAndSet(false, true)) {
            try {
//                log.info("Resolving job-queue via schedule.");
                publishJobsInQueue();
            } finally {
                isJobProcessing.set(false);
            }
        }
    }





    /**
     * Adds jobs to RabbitMQ for processing.
     * This method retrieves jobs from the database and adds them to the RabbitMQ queue based on their status and schedule.
     */
    public void publishJobsInQueue() {

        jobRepository.findAll()
                .stream()
                .filter(model -> !Arrays.asList(JobStatus.RUNNING, JobStatus.NO_INSTANCE, JobStatus.SUCCESS).contains(model.getStatus())
                        && jobProcessor.isJobAttemptsNotExceeded(model.getAttempts())
                )
                .filter(jobProcessor::isJobReadyToRun)
                .map(jobProcessor::enqueueJob)
                .filter(Objects::nonNull)
                .forEach(job -> log.info("Added Job ({}) in the queue. [{}]", job.getLeft(), job.getRight()));

    }





    /**
     * Saves the provided multipart file locally in the specified directory.
     *
     * @param directory The directory where the file will be saved.
     * @param file      The multipart file to be saved.
     * @return A FileDataModel object representing the saved file.
     * @throws InvalidDataException If the provided file is null or empty, if the filename or extension is invalid,
     *                              or if any error occurs during the file saving process.
     */
    @SneakyThrows
    public FileDataModel saveFileLocally(
            String directory,
            MultipartFile file
    ) {
        // Validate the file
        if (null == file || file.isEmpty())
            throw new InvalidDataException("No file found.");

        // Validate the filename and extension
        String fileExtension = fileUtils.getExtension(file.getOriginalFilename());
        if (textHelper.isBlank(file.getOriginalFilename()) || null == fileExtension)
            throw new InvalidDataException("Invalid filename or extension.");

        String randomId = UUID.randomUUID().toString().replace("-", "");

        // Generate a unique filename using UUID
        String fileName = generateRandomFileName() + "." + fileExtension;

        // Save the file to the specified directory
        Path filePath = Paths.get(directory, fileName);
        file.transferTo(filePath);

        // Create and return a FileDataModel representing the saved file
        return FileDataModel.builder()
                .fileName(fileName)
                .fileOriginal(textHelper.sanitize(file.getOriginalFilename()))
                .fileType(file.getContentType())
                .filePath(filePath.toString())
                .fileCategory(FileCategory.INPUT)
                .build();
    }


    @Override
    public APIResponse<?> updateSchedule(
            LocalTime localTime,
            TokenValidationResponse validationResponse
    ) {

        if(null == localTime)
            throw new InvalidDataException("Please provide a valid schedule time.");

        Optional<JobScheduleTimingModel> scheduleTimingModel = jobScheduleTimingRepository.findById(1L);

        if(scheduleTimingModel.isPresent()){
            scheduleTimingModel.get().setScheduleTime(localTime);
            jobScheduleTimingRepository.save(scheduleTimingModel.get());
        }else {
            jobScheduleTimingRepository.save(
                    JobScheduleTimingModel.builder()
                            .jobType(JobType.REPORT_GENERATOR)
                            .scheduleTime(LocalTime.of(16, 15)) // 18:00 IST / 12:30 as per UTC in db
                            .build()
            );
        }

        return APIResponse.builder()
                .message("Schedule time updated successfully")
                .build();
    }



}
