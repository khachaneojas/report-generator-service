package com.service.report.generator.exception;

import com.service.report.generator.dto.APIResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final Throwable exception;
    private final APIResponse<Object> response;

    public BaseException(HttpStatus status) {
        super();
        this.status = status;
        this.exception = null;
        this.response = null;
    }

    public BaseException(HttpStatus status, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.exception = null;
        this.response = APIResponse.builder().error(errorMessage).build();
    }

    public <T> BaseException(HttpStatus status, T data) {
        super();
        this.status = status;
        this.exception = null;
        this.response = APIResponse.builder().data(data).build();
    }

    public BaseException(HttpStatus status, Throwable exception) {
        super(exception.getMessage());
        this.status = status;
        this.exception = exception;
        this.response = APIResponse.builder().error(exception.getMessage()).build();
    }

    public BaseException(HttpStatus status, Throwable exception, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.exception = exception;
        this.response = APIResponse.builder().error(errorMessage).build();
    }

    public <T> BaseException(HttpStatus status, String errorMessage, T data) {
        super(errorMessage);
        this.status = status;
        this.exception = null;
        this.response = APIResponse.builder().error(errorMessage).data(data).build();
    }

    public <T> BaseException(HttpStatus status, Throwable exception, String errorMessage, T data) {
        super(errorMessage);
        this.status = status;
        this.exception = exception;
        this.response = APIResponse.builder().error(errorMessage).data(data).build();
    }

    public BaseException(HttpStatus status, APIResponse<Object> response) {
        super(response.getError());
        this.status = status;
        this.exception = null;
        this.response =  response;
    }

    public BaseException(HttpStatus status, Throwable exception, APIResponse<Object> response) {
        super(response.getError());
        this.status = status;
        this.exception = exception;
        this.response =  response;
    }

}
