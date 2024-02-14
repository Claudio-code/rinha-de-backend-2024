package com.rinha.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
@Table("clientes")
public class Clientes {
    @Id
    private Integer id;
    private String nome;
    private Integer limite;
    private Integer saldo;

    public void adicionarSaldo(final Integer valor) {
        saldo += valor;
    }

    public void removerSaldo(final Integer valor) {
        saldo -= valor;
    }
}
