package com.rinha.backend.repository;

import com.rinha.backend.entity.Transacoes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TransacoesPageableRepository extends ReactiveSortingRepository<Transacoes, Integer> {
    Flux<Transacoes> findByClienteID(Integer clienteID, Pageable pageable);
}
