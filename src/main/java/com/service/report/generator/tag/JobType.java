package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobType {

    REPORT_GENERATOR(0);

    private final int value;
}
