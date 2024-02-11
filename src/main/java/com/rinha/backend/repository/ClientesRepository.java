package com.rinha.backend.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.rinha.backend.entity.Clientes;

public interface ClientesRepository extends ReactiveCrudRepository<Clientes, Integer> {
}
