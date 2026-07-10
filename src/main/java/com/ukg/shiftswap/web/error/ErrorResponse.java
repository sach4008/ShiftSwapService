package com.ukg.shiftswap.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/** The single error shape returned by every endpoint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String resolvedStatus,
        Long resolvedBy
) {

    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path, null, null);
    }

    public static ErrorResponse withResolution(HttpStatus status, String code, String message, String path,
                                                String resolvedStatus, Long resolvedBy) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path,
                resolvedStatus, resolvedBy);
    }
}
