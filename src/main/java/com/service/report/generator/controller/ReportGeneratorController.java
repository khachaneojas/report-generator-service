package com.service.report.generator.controller;


import com.service.report.generator.annotation.Auditor;
import com.service.report.generator.dto.APIResponse;
import com.service.report.generator.dto.JwtTokenResponse;
import com.service.report.generator.dto.TokenValidationResponse;
import com.service.report.generator.dto.payload.LoginRequest;
import com.service.report.generator.service.ReportGeneratorService;
import com.service.report.generator.tag.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;


@RestController
@RequestMapping(path = "/api/generator")
@RequiredArgsConstructor
public class ReportGeneratorController {

    private final ReportGeneratorService reportGeneratorService;



    /**
     * Uploads files for processing. The main file is mandatory, while reference files are optional.
     * Validates the uploaded files, processes them based on their type, and schedules a job for report generation.
     *
     * @param mainfile The main file to be uploaded (required).
     * @param reference1 An optional reference file 1.
     * @param reference2 An optional reference file 2.
     * @param validationResponse The token validation response containing user details.
     * @return ResponseEntity containing the API response with status and message.
     */
    @PostMapping("/upload-file")
    @Operation(
            summary = "UPLOAD FILE",
            description = "Uploads files for processing. The main file is mandatory, while reference files are optional.\n\n" +
                    "Access Control:\n" +
                    "This endpoint requires a valid token validation response for authorization.\n\n" +
                    "Endpoint Workflow:\n" +
                    "1. When a request is made to this endpoint, the server validates the token validation response.\n" +
                    "2. The server processes the uploaded files based on their type and validates them.\n" +
                    "3. If validation is successful, the server schedules a job for report generation.\n" +
                    "4. The server generates an APIResponse indicating the success or failure of the file upload and job scheduling.\n" +
                    "5. The APIResponse is returned in a ResponseEntity with HTTP status OK (200).\n",
            tags = {"UPLOAD"}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Files uploaded and processed successfully. Job for report generation scheduled.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIResponse.class)
                    )
            )
    })
    @Auditor(allowedRoles = UserRole.ADMIN)
    public ResponseEntity<?> uploadFile(
            @Parameter(
                    name = "main",
                    description = "The main file to be uploaded. This file is mandatory.",
                    required = true
            )
            @RequestParam(name = "main", required = false) MultipartFile mainfile,

            @Parameter(
                    name = "ref-1",
                    description = "Optional reference file 1.",
                    required = false
            )
            @RequestParam(name = "ref-1", required = false) MultipartFile reference1,

            @Parameter(
                    name = "ref-2",
                    description = "Optional reference file 2.",
                    required = false
            )
            @RequestParam(name = "ref-2", required = false) MultipartFile reference2,

            @Parameter(
                    description = "Token validation response containing user details.",
                    required = true
            )
            TokenValidationResponse validationResponse
    ){
        APIResponse<?> response = reportGeneratorService.uploadFile(mainfile,reference1,reference2, validationResponse);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }




    /**
     * Triggers the report generation process for a specified job. This endpoint starts the processing of the job identified by the given job ID.
     * The method validates the job ID, processes the files associated with the job, and generates the output file.
     * Upon successful processing, it returns a response with the output file name.
     *
     * @param jobUid The unique identifier for the job to be processed. This path variable is required.
     * @return ResponseEntity containing the API response with status and message upon successful job processing.
     */
    @PostMapping(path = "/execute/{jobUid}")
    @Operation(
            summary = "TRIGGER REPORT GENERATION",
            description = "Triggers the report generation process for a specified job. This endpoint starts the processing of the job identified by the given job ID.\n\n" +
                    "Access Control:\n" +
                    "This endpoint requires a valid token validation response for authorization.\n\n" +
                    "Endpoint Workflow:\n" +
                    "1. When a request is made to this endpoint, the server validates the provided job ID.\n" +
                    "2. The server processes the files associated with the specified job ID.\n" +
                    "3. The server generates the output file based on the job's files.\n" +
                    "4. The server wraps the output file information in an APIResponse.\n" +
                    "5. The APIResponse is returned in a ResponseEntity with HTTP status OK (200).\n",
            tags = {"REPORT"}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job processed successfully and output file generated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIResponse.class)
                    )
            )
    })
    @Auditor(allowedRoles = UserRole.ADMIN)
    public ResponseEntity<?> triggerReportGeneration(
            @Parameter(
                    name = "jobUid",
                    description = "The unique identifier for the job to be processed. This is required to locate the job for execution.",
                    required = true
            )
            @PathVariable(value = "jobUid") String jobUid
    ){
        APIResponse<?> response = reportGeneratorService.triggerReportGeneration(jobUid);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }




    /**
     * Signs in a user by validating the provided login credentials and generating a JWT token for authentication.
     * This endpoint processes the login request, checks the provided credentials, and returns a JWT token if the credentials are valid.
     *
     * @param loginRequest The request containing the user's login credentials. This includes the user's email and password.
     * @return ResponseEntity containing the JWT token response upon successful user authentication. The response includes the generated JWT token for authenticated access.
     */
    @PostMapping(path = "/login")
    @Operation(
            summary = "USER SIGN-IN",
            description = "Signs in a user by validating the provided login credentials and generating a JWT token for authentication.\n\n" +
                    "Endpoint Workflow:\n" +
                    "1. When a request is made to this endpoint, the server validates the provided login credentials from the loginRequest.\n" +
                    "2. The server sanitizes the input parameters (email and password).\n" +
                    "3. The server retrieves the user model based on the provided email.\n" +
                    "4. The server checks if the user exists and if the provided password matches the stored password.\n" +
                    "5. If authentication is successful, the server generates a JWT token.\n" +
                    "6. The JWT token is returned in a JwtTokenResponse within a ResponseEntity with HTTP status OK (200).\n",
            tags = {"AUTHENTICATION"}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User authenticated successfully and JWT token generated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtTokenResponse.class)
                    )
            )
    })
    @Auditor(auditJwt = false)
    public ResponseEntity<?> signInUser(
            @Parameter(
                    description = "The request containing the user's login credentials.",
                    required = true
            )
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        // Sign in the user by validating the provided login credentials and generating a JWT token.
        JwtTokenResponse response = reportGeneratorService.signInUser(loginRequest);
        // Return the JWT token response upon successful user authentication.
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }




    /**
     * Updates the job schedule with the specified time. This endpoint allows the administrator to set or update the schedule time for job execution.
     * It validates the provided schedule time and updates the job schedule in the database.
     *
     * @param localTime The new schedule time to be set for the job. This parameter is required and should be a valid LocalTime object.
     * @param validationResponse The token validation response containing user details. This is used to authorize the request.
     * @return ResponseEntity containing the API response with status and message upon successful update of the schedule time.
     */
    @PatchMapping(path = "/update/schedule")
    @Operation(
            summary = "UPDATE JOB SCHEDULE",
            description = "Updates the job schedule with the specified time. This endpoint allows the administrator to set or update the schedule time for job execution.\n\n" +
                    "Access Control:\n" +
                    "This endpoint requires a valid token validation response for authorization.\n\n" +
                    "Endpoint Workflow:\n" +
                    "1. When a request is made to this endpoint, the server validates the provided schedule time (localTime).\n" +
                    "2. The server checks if a job schedule record exists in the database.\n" +
                    "3. If a record exists, the server updates the schedule time with the provided value.\n" +
                    "4. If no record exists, the server creates a new job schedule record with the provided time.\n" +
                    "5. The server returns an APIResponse indicating the success of the schedule update.\n",
            tags = {"SCHEDULE"}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Schedule time updated successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = APIResponse.class)
                    )
            )
    })
    @Auditor(allowedRoles = UserRole.ADMIN)
    public ResponseEntity<?> updateSchedule(
            @Parameter(
                    name = "update",
                    description = "The new schedule time to be set for the job. This parameter is required and should be a valid LocalTime object.",
                    required = true
            )
            @RequestParam(name = "update") LocalTime localTime,
            @Parameter(
                    description = "Token validation response containing user details.",
                    required = true
            )
            TokenValidationResponse validationResponse
    ){
        // Update the job schedule with the provided time.
        APIResponse<?> response = reportGeneratorService.updateSchedule(localTime, validationResponse);

        // Return the response indicating success of the schedule update.
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }



}
