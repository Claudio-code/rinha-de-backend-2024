package com.rinha.backend.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.rinha.backend.entity.Transacoes;

import reactor.core.publisher.Flux;

public interface TransacoesRepository extends ReactiveCrudRepository<Transacoes, Integer> {
    Flux<Transacoes> findByClienteID(Integer clienteID);
}
