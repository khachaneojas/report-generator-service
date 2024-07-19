package com.service.report.generator.adviser;

import com.service.report.generator.dto.APIResponse;
import com.service.report.generator.exception.BaseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//@RestControllerAdvice
public class ExceptionAdviser {

    @ExceptionHandler(value = BaseException.class)
    public ResponseEntity<APIResponse<Object>> handleCustomExceptions(BaseException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(exception.getResponse());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public APIResponse<Void> handleUnhandledExceptions(Exception exception) {
        Logger.log(exception.getClass(), ExceptionUtils.getStackTrace(exception), LogLevel.ERROR);
        return APIResponse.<Void>builder().error("Uh-oh! Something went wrong. :(").build();
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public APIResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return defaultResponse(exception);
    }

    private APIResponse<Void> defaultResponse(Throwable throwable) {
        return APIResponse.<Void>builder().error(throwable.getMessage()).build();
    }

}
