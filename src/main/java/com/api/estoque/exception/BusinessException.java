package com.api.estoque.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // Erros de negócio geralmente retornam 400
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}