package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationType {

    DEFAULT(0),
    SPACE_BETWEEN(1),
    COMMA_SEPARATED(2),
    MATHEMATICAL(3),
    FUNCTION(4);
    private final int value;

}
