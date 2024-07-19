package com.service.report.generator.dto;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenValidationResponse {
    private Long pid;
    private String uid;
    private boolean adminRole;
}
