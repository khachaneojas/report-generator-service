package com.service.report.generator.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputFileDTO {

    String outputFileName;
    String filePath;
    String fileType;

}
