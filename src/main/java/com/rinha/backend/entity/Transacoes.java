package com.rinha.backend.entity;

import java.time.Instant;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Cacheable
@Table("transacoes")
public class Transacoes {
    @Id
    @JsonIgnore
    private Integer id;
    private String descricao;
    @Column("cliente_id")
    @JsonIgnore
    private Integer clienteID;
    private Integer valor;
    private String tipo;
    @Column("realizada_em")
    @JsonProperty("realizada_em")
    private Instant realizadaEm;
}
