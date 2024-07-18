package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileType {

    MAIN(0),
    REFERENCE(1);

    private final int value;
}
