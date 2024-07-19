package com.service.report.generator.exception;

import org.springframework.http.HttpStatus;

public class EmailDispatcherException extends BaseException {
    public EmailDispatcherException(String emailAddress) {
        super(HttpStatus.BAD_REQUEST, buildErrorMessage(emailAddress));
    }
    public EmailDispatcherException(String emailAddress, Exception exception) {
        super(HttpStatus.BAD_REQUEST, exception, buildErrorMessage(emailAddress));
    }
    private static String buildErrorMessage(String emailAddress) {
        return "Oops, it seems like your email (" + emailAddress + ") isn't valid because it's nowhere to be found online.";
    }
}
