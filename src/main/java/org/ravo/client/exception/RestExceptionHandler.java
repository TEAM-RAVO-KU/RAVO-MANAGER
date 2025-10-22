package org.ravo.client.exception;

import org.ravo.client.controller.BankApiController;
import org.ravo.client.controller.BankController;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {BankApiController.class, BankController.class})
public class RestExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> handleDb(DataAccessException ex) {
        ex.getMostSpecificCause();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "ok", false,
                        "error", "DB_CONNECTION_ERROR",
                        "message", ex.getMostSpecificCause().getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "ok", false,
                        "error", "GENERIC_ERROR",
                        "message", ex.getMessage()
                ));
    }
}
