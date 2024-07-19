package com.service.report.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.service.report.generator.dto.FileDataDTO;
import com.service.report.generator.dto.JwtTokenResponse;
import com.service.report.generator.dto.payload.LoginRequest;
import com.service.report.generator.entity.*;
import com.service.report.generator.exception.BadCredentialsException;
import com.service.report.generator.repository.*;
import com.service.report.generator.dto.APIResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final String ERROR_GENERIC_MESSAGE = "Oops! Something went wrong.";
    private static final String JOB_NAME = "Report Generation";
    private static final String JOB_DESCRIPTION = "This job is intended to generate a report using certain transformation rules for the specified files";
    private static final long MAX_TOTAL_FILES_SIZE = 3072L * 1024L * 1024L;
    private final AtomicBoolean isJobProcessing = new AtomicBoolean(false);

    @Value("${app.upload.dir.doc}")
    private String documentDirectory;




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

        // Ensure the existense of required schedule time for scheduling a report generation job
        if(0 == jobScheduleTimingRepository.count()){
            jobScheduleTimingRepository.save(
                    JobScheduleTimingModel.builder()
                            .jobType(JobType.REPORT_GENERATOR)
                            .scheduleTime(LocalTime.of(18, 0)) //12:30 as per UTC
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


    public static String formattedNOW() {
        return Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
    }



    @Override
    public APIResponse<?> uploadFile(
            MultipartFile mainfile,
            MultipartFile reference1,
            MultipartFile reference2
    ) {

        if(null == mainfile || mainfile.isEmpty())
            throw new InvalidDataException("To proceed, provide with a input file");

        boolean isReference1Present = null != reference1 && !reference1.isEmpty();
        boolean isReference2Present = null != reference2 && !reference2.isEmpty();

        Map<FileType, List<MultipartFile>> multipartFileMap = new LinkedHashMap<>();

        List<MultipartFile> mainMultipartFile = new ArrayList<>();
        mainMultipartFile.add(mainfile);
        multipartFileMap.put(FileType.MAIN,mainMultipartFile);

        List<MultipartFile> referenceMultipartFile = new ArrayList<>();
        if(isReference1Present)
            referenceMultipartFile.add(reference1);

        if(isReference2Present)
            referenceMultipartFile.add(reference2);

        if(!referenceMultipartFile.isEmpty())
            multipartFileMap.put(FileType.REFERENCE, referenceMultipartFile);

        Map<FileType, List<FileDataDTO>> fileDataDTOMap= updateDocumentByFileDataModel(
                multipartFileMap,
                documentDirectory
        );

        FileDataDTO mainFileDTO = fileDataDTOMap.get(FileType.MAIN).get(0);

        FileDataDTO reference1DTO = null;
        FileDataDTO reference2DTO = null;

        List<FileDataDTO> referenceDTOList = fileDataDTOMap.get(FileType.REFERENCE);
        if(null != referenceDTOList && !referenceDTOList.isEmpty()){
            reference1DTO = referenceDTOList.get(0);

            if(1 < referenceDTOList.size())
                reference2DTO = referenceDTOList.get(1);
        }

        FileExtensionType fileExtensionType = getFileType(mainFileDTO.getFileExtension(),
                null != reference1DTO ? reference1DTO.getFileExtension() : null,
                null != reference2DTO ? reference2DTO.getFileExtension() : null);

        switch (fileExtensionType){

            case CSV -> {

                Map<FileType, List<Long>> fileMap = new LinkedHashMap<>();

                // Adding main file ID to the map
                List<Long> mainFileIdList = new ArrayList<>();
                mainFileIdList.add(mainFileDTO.getFileDataModel().getFileId());
                fileMap.put(FileType.MAIN, mainFileIdList);

                // Adding reference file IDs to the map
                List<Long> referenceFileIdList = new ArrayList<>();
                if (null != reference1DTO)
                    referenceFileIdList.add(reference1DTO.getFileDataModel().getFileId());

                if (null != reference2DTO)
                    referenceFileIdList.add(reference2DTO.getFileDataModel().getFileId());

                if(!referenceFileIdList.isEmpty())
                    fileMap.put(FileType.REFERENCE, referenceFileIdList);

                scheduleJobForReportGeneration(fileMap);

                return APIResponse.builder()
                        .message("Files successfully uploaded.")
                        .build();
            }

        }

        return APIResponse.builder()
                .message("Currently we only serve csv files")
                .build();

    }



    @Override
    public JwtTokenResponse signInUser(
            LoginRequest loginRequest
    ) {
        // Sanitize input parameters
        String email = textHelper.sanitize(loginRequest.getEmail());
        String password = textHelper.sanitize(loginRequest.getPassword());

        // Retrieve user model by email or user UID
        UserModel userModel = userRepository.findByEmail(email);
        // Check if the user exists and the password matches
        if (null == userModel || !passwordEncoder.matches(password, userModel.getPassword()))
            throw new BadCredentialsException();

        // Generate JWT token for the user
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
     * Schedules a job to change the academic status of students.
     *
     * @param fileMap the map containing the FileType and their id respectively
     * @throws NullPointerException if the fileMap is null
     */
    private void scheduleJobForReportGeneration(Map<FileType, List<Long>> fileMap){

        // Ensure that the response is not null
        Objects.requireNonNull(fileMap);

        String jobData;
        try {
            jobData = jsonConverter.convertMapToJsonString(fileMap);
        } catch (JsonProcessingException exception) {
            throw new InvalidDataException(ERROR_GENERIC_MESSAGE);
        }

        JobScheduleTimingModel jobScheduleTimingModel = jobScheduleTimingRepository.findByJobType(JobType.REPORT_GENERATOR);

        LocalTime localTime = jobScheduleTimingModel.getScheduleTime();

        Instant scheduleInstant = getNextScheduleInstant(localTime);

        // Build the JobModel object
        JobModel job = JobModel.builder()
                .name(JOB_NAME)
                .description(JOB_DESCRIPTION)
                .attempts(0)
                .scheduleType(ScheduleType.ONCE)
                .status(JobStatus.QUEUED)
                .executeAt(scheduleInstant)
                .jobType(JobType.REPORT_GENERATOR)
                .jsonData(jobData)
                .build();

        // Schedule the job using the job scheduler
        jobRepository.save(job);

    }



    public static Instant getNextScheduleInstant(LocalTime localTime) {
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

                if (null == fileExtension || file.getSize() > MAX_TOTAL_FILES_SIZE)
                    deleteLocallySavedFiles(locallySavedFiles);

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

        throw new InvalidDataException("Invalid file found.");
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
    private void publishJobsInQueue() {

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
        String fileName = randomId + "-" + formattedNOW() + "." + fileExtension;

        // Save the file to the specified directory
//		try {
        Path filePath = Paths.get(directory, fileName);
        file.transferTo(filePath);

        // Create and return a FileDataModel representing the saved file
        return FileDataModel.builder()
                .fileName(fileName)
                .fileOriginal(textHelper.sanitize(file.getOriginalFilename()))
                .fileType(file.getContentType())
                .filePath(filePath.toString())
                .build();

//		} catch (Exception exception) {
//			throw new InvalidDataException(exception.getMessage());
//		}

    }
    


}
