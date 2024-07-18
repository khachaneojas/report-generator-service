package com.service.report.generator.dto;

import com.service.report.generator.entity.FileDataModel;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDataDTO {

    private String fileExtension;
    private FileDataModel fileDataModel;

}
