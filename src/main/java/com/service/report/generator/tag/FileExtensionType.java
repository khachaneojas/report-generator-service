package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileExtensionType {

    CSV(0),
    EXCEL(1),
    JSON(2);

    private final int value;
}
