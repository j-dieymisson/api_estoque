package com.api.estoque.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice // Diz ao Spring que esta classe vai interceptar exceções de todos os controllers
public class GlobalExceptionHandler {

    // Define um DTO simples para a resposta de erro
    private record ErrorResponse(String message) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Verifica se o erro é por causa de uma 'UNIQUE constraint'
        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            String constraintName = ((org.hibernate.exception.ConstraintViolationException) ex.getCause()).getConstraintName();

            // Verificamos se o nome da constraint é o que esperamos para nomes únicos (pode variar)
            // Exemplo: uk_nome_categoria, idx_nome_unico, etc.
            if (constraintName != null && constraintName.toLowerCase().contains("unique")) {
                // Mensagem de erro amigável
                String errorMessage = "Já existe um registo com os dados fornecidos. Por favor, verifique e tente novamente.";

                // Podemos ser mais específicos se quisermos
                if (constraintName.contains("categorias")) {
                    errorMessage = "Já existe uma categoria com este nome.";
                } else if (constraintName.contains("usuarios")) {
                    errorMessage = "Já existe um funcionário com este nome ou email.";
                }

                return new ResponseEntity<>(Map.of("message", errorMessage), HttpStatus.BAD_REQUEST);
            }
        }

        // Se for outro tipo de erro de integridade, devolve um erro 500 genérico
        return new ResponseEntity<>(Map.of("message", "Erro de integridade de dados."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}