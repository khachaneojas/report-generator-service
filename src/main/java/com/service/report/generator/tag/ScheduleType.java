package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ScheduleType {
    EVERYDAY(0),
    SPECIFIED_CUSTOM_DATES(1),
    SPECIFIED_WEEKDAYS(2),
    ONCE(3);
    private final int value;
}
