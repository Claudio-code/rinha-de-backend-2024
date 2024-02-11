package com.rinha.backend.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rinha.backend.entity.Transacoes;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ExtratoResponseDto {
    private ExtratoSaldoDto saldo;
    @JsonProperty("ultimas_transacoes")
    private List<Transacoes> transacoes;

    @Getter
    @Builder
    public static class ExtratoSaldoDto {
        private Integer total;
        @JsonProperty("data_extrato")
        private Instant dataExtrato;
        private Integer limite;
    }
}
