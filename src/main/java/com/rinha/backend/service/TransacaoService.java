package com.rinha.backend.service;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.rinha.backend.dto.ExtratoResponseDto;
import com.rinha.backend.dto.TransacaoRequestDto;
import com.rinha.backend.dto.TransacaoResponseDto;
import com.rinha.backend.entity.Transacoes;
import com.rinha.backend.enums.TipoTransacao;
import com.rinha.backend.exceptions.SaldoInconsistenteException;
import com.rinha.backend.repository.ClientesRepository;
import com.rinha.backend.repository.TransacoesRepository;

import java.time.Instant;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class TransacaoService {
    private final ClientesRepository clientesRepository;
    private final TransacoesRepository transacaoRepository;

    public synchronized Mono<TransacaoResponseDto> fazerTransacao(final int clienteID, final TransacaoRequestDto transacaoDto) {
        final var transacoes = Transacoes.builder()
                .clienteID(clienteID)
                .tipo(transacaoDto.tipo())
                .descricao(transacaoDto.descricao())
                .valor(transacaoDto.valor())
                .realizadaEm(Instant.now())
                .build();
        final var trasacaoPublisher = transacaoRepository.save(transacoes);
        return clientesRepository.findById(clienteID)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(cliente -> {
                    if (Objects.equals(TipoTransacao.CREDITO.getTipo(), transacaoDto.tipo())) {
                        cliente.adicionarSaldo(transacaoDto.valor());
                    } else {
                        cliente.removerSaldo(transacaoDto.valor());
                    }
                    if (cliente.verificarSeSaldoEstaInconsistente()) {
                        return Mono.error(new SaldoInconsistenteException());
                    }
                    return Mono.just(cliente);
                })
                .flatMap(clientesRepository::save)
                .flatMap(trasacaoPublisher::thenReturn)
                .flatMap(cliente -> Mono.just(new TransacaoResponseDto(cliente.getLimite(), cliente.getSaldo())));
    }

    public Mono<ExtratoResponseDto> extrato(final int clienteID) {
        return clientesRepository.findById(clienteID)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .map(cliente -> {
                    final var extratoSaldo = ExtratoResponseDto.ExtratoSaldoDto.builder()
                            .limite(cliente.getLimite())
                            .total(cliente.getSaldo())
                            .dataExtrato(Instant.now())
                            .build();
                    return ExtratoResponseDto.builder()
                            .saldo(extratoSaldo);
                })
                .flatMap(extrato -> {
                    return transacaoRepository.findByClienteID(clienteID)
                            .collectList()
                            .map(transacoes -> extrato.transacoes(transacoes).build());
                });
    }
}
