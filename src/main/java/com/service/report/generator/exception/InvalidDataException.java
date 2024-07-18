package com.service.report.generator.exception;

import org.springframework.http.HttpStatus;

public class InvalidDataException extends BaseException {
    public InvalidDataException(String errorMessage) {
        super(HttpStatus.BAD_REQUEST, errorMessage);
    }
    public InvalidDataException(Exception exception) {
        super(HttpStatus.BAD_REQUEST, exception);
    }
    public InvalidDataException(Exception exception, String errorMessage) {
        super(HttpStatus.BAD_REQUEST, exception, errorMessage);
    }
    public InvalidDataException(String errorMessage, Object data) {
        super(HttpStatus.BAD_REQUEST, errorMessage, data);
    }
    public InvalidDataException(Exception exception, String errorMessage, Object data) {
        super(HttpStatus.BAD_REQUEST, exception, errorMessage, data);
    }
}
