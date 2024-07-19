package com.service.report.generator.exception;

import org.springframework.http.HttpStatus;

public class AnonymityException extends BaseException {
    public AnonymityException() {
        super(HttpStatus.NOT_FOUND);
    }
}
