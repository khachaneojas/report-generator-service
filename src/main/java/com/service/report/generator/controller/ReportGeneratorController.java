package com.service.report.generator.controller;


import com.service.report.generator.dto.APIResponse;
import com.service.report.generator.service.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalTime;

@RestController
@RequestMapping(path = "/api/generator")
@RequiredArgsConstructor
public class ReportGeneratorController {

    private final ReportGeneratorService reportGeneratorService;




    @PostMapping("/upload-file")
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


}
