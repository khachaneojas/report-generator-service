package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    DEFAULT(0),
    ADMIN(1);

    private final int value;
}
