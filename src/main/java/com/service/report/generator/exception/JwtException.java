package com.service.report.generator.exception;

import org.springframework.http.HttpStatus;

public class JwtException extends BaseException {
    public JwtException(String errorMessage) {
        super(HttpStatus.FORBIDDEN, errorMessage);
    }
    public JwtException(Exception exception) {
        super(HttpStatus.FORBIDDEN, exception);
    }
    public JwtException(Exception exception, String errorMessage) {
        super(HttpStatus.FORBIDDEN, exception, errorMessage);
    }
    public JwtException(String errorMessage, Object data) {
        super(HttpStatus.FORBIDDEN, errorMessage, data);
    }
    public JwtException(Exception exception, String errorMessage, Object data) {
        super(HttpStatus.FORBIDDEN, exception, errorMessage, data);
    }
}
