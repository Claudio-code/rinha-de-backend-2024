package com.rinha.backend.controller;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rinha.backend.dto.ExtratoResponseDto;
import com.rinha.backend.dto.TransacaoRequestDto;
import com.rinha.backend.dto.TransacaoResponseDto;
import com.rinha.backend.enums.TipoTransacao;
import com.rinha.backend.exceptions.TipoErradoException;
import com.rinha.backend.service.TransacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("clientes")
public class ClientesController {
    private final TransacaoService service;

    @PostMapping("{id}/transacoes")
    @Transactional
    public Mono<TransacaoResponseDto> fazerTransacao(
            @PathVariable("id") @Valid final Integer clienteID,
            @RequestBody @Valid final TransacaoRequestDto transacaoRequestDto) throws TipoErradoException {
        if (!TipoTransacao.isValidType(transacaoRequestDto.tipo()) ||
                transacaoRequestDto.tipo().chars().count() > 1 ||
                transacaoRequestDto.descricao().chars().count() > 10) {
            throw new TipoErradoException();
        }
        return service.fazerTransacao(clienteID, transacaoRequestDto);
    }

    @GetMapping("{id}/extrato")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Mono<ExtratoResponseDto> extrato(@PathVariable("id") @Valid final Integer clienteID) {
        return service.extrato(clienteID);
    }
}
