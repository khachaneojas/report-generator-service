package com.service.report.generator.service;

import com.service.report.generator.dto.APIResponse;
import com.service.report.generator.dto.JwtTokenResponse;
import com.service.report.generator.dto.payload.LoginRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ReportGeneratorService {

    APIResponse<?> uploadFile(MultipartFile mainfile, MultipartFile reference1, MultipartFile reference2);

    JwtTokenResponse signInUser(LoginRequest loginRequest);
}
