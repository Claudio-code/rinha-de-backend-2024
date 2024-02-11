package com.rinha.backend.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoTransacao {
    CREDITO("c"),
    DEBITO("d");

    private String tipo;


    public static boolean isValidType(String type) {
        return Arrays.stream(TipoTransacao.values())
                     .anyMatch(t -> t.tipo.equals(type));
    }
}
