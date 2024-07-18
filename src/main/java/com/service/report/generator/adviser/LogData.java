package com.service.report.generator.adviser;

import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@NoArgsConstructor
public class LogData {
    public static final String JOIN_PATTERN = "] --- [";
    private String httpMethod;
    private String endPoint;
    private String id;
    private String remoteIp;
    private String remoteHost;
    private String response;

    @Override
    public String toString() {
        return ("["
                + httpMethod + JOIN_PATTERN
                + endPoint + JOIN_PATTERN
                + id + JOIN_PATTERN
                + remoteIp + JOIN_PATTERN
                + remoteHost + (null == response ? "]" : JOIN_PATTERN + response + "]"));
    }
}
