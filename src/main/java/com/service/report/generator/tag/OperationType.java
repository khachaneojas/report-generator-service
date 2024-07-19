package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationType {

    STRING(0),
    MATHEMATICAL(1),
    FUNCTION(2);
    private final int value;

}
