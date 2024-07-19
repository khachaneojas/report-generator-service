package com.service.report.generator.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum DeviceAddressType {
    IP(0),
    MAC(1);
    private final int value;
}
