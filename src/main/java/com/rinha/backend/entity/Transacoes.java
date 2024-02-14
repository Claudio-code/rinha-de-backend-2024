package com.rinha.backend.entity;

import java.time.Instant;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
@Table("transacoes")
public record Transacoes(
    @Id
    @JsonIgnore
    Integer id,
    String descricao,
    @Column("cliente_id")
    @JsonIgnore
    Integer clienteID,
    Integer valor,
    String tipo,
    @Column("realizada_em")
    @JsonProperty("realizada_em")
    Instant realizadaEm
) {
}
