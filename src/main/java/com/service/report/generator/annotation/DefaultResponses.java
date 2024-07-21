package com.service.report.generator.annotation;

import com.service.report.generator.dto.APIResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = APIResponse.class), mediaType = "application/json")),
        @ApiResponse(responseCode = "204", content = @Content(schema = @Schema(example = "[]", type = "List<>"), mediaType = "application/json")),
        @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(example = "{\n" +
                "  \"error\": \"Bad Request.\"\n" +
                "}"), mediaType = "application/json")),
        @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(example = "{\n" +
                "  \"error\": \"Unauthorized access. You do not have the necessary permissions to access this resource.\"\n" +
                "}"), mediaType = "application/json")),
        @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(example = "{\n" +
                "  \"error\": \"Not Found.\"\n" +
                "}"), mediaType = "application/json")),
        @ApiResponse(responseCode = "415", content = @Content(schema = @Schema(example = "{\n" +
                "  \"error\": \"Unsupported Media Type.\"\n" +
                "}"), mediaType = "application/json"))
})
public @interface DefaultResponses {}
