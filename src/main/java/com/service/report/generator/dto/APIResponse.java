package com.service.report.generator.dto;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class APIResponse<T> {
    private T data;
    private String message;
    private String error;
}
