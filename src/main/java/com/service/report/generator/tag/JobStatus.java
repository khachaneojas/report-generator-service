package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum JobStatus {
    SUCCESS(0),
    FAILED(1),
    QUEUED(2),
    RUNNING(3),
    NO_INSTANCE(4);
    private final int value;
}
