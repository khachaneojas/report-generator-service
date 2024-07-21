package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileCategory {

    INPUT(0),
    OUTPUT(1);

    private final int value;
}
