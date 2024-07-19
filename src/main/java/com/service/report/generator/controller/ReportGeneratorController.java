package com.service.report.generator.controller;


import com.service.report.generator.annotation.Auditor;
import com.service.report.generator.dto.APIResponse;
import com.service.report.generator.dto.JwtTokenResponse;
import com.service.report.generator.dto.payload.LoginRequest;
import com.service.report.generator.service.ReportGeneratorService;
import com.service.report.generator.tag.UserRole;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping(path = "/api/generator")
@RequiredArgsConstructor
public class ReportGeneratorController {

    private final ReportGeneratorService reportGeneratorService;




    @PostMapping("/upload-file")
    @Auditor(allowedRoles = UserRole.ADMIN)
    public ResponseEntity<?> uploadFile(
            @RequestParam(name = "main", required = false) MultipartFile mainfile,
            @RequestParam(name = "ref-1", required = false) MultipartFile reference1,
            @RequestParam(name = "ref-2", required = false) MultipartFile reference2
    ){
        APIResponse<?> response = reportGeneratorService.uploadFile(mainfile,reference1,reference2);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }










    /**
     * Sign in a user by validating the provided login credentials and generating a JWT token for authentication.
     *
     * @param loginRequest The request containing the user's login credentials.
     * @return ResponseEntity containing the JWT token response upon successful user authentication.
     */
    @PostMapping(path = "/login")
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




}
