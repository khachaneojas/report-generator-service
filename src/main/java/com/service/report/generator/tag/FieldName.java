package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FieldName {

    OUTFIELD1(0),
    OUTFIELD2(1),
    OUTFIELD3(2),
    OUTFIELD4(3),
    OUTFIELD5(4),
    OUTFIELD6(5);

    private final int value;

}
