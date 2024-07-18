package com.service.report.generator.service;

import com.service.report.generator.dto.APIResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReportGeneratorService {

    APIResponse<?> uploadFile(MultipartFile mainfile, MultipartFile reference1, MultipartFile reference2);

}
