package com.rinha.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TransacaoRequestDto(
        @NotNull Integer valor,
        @NotNull @NotEmpty String tipo,
        @NotNull @NotEmpty String descricao) {
}
