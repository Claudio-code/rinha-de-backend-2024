package com.rinha.backend.controller;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.rinha.backend.exceptions.SaldoInconsistenteException;
import com.rinha.backend.exceptions.TipoErradoException;

import reactor.core.publisher.Mono;

@RestControllerAdvice
class BaseController {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<Void> handleValidationExceptions() {
        return Mono.empty();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Void> handleNotfound() {
        return Mono.empty();
    }

    @ExceptionHandler(TipoErradoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<Void> handleTransactionErrorType() {
        return Mono.empty();
    }

    @ExceptionHandler(SaldoInconsistenteException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<Void> handleInconsistentBalance() {
        return Mono.empty();
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<Void> handleRequestBodyParams() {
        return Mono.empty();
    }
}
