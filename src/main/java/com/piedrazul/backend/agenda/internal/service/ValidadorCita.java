package com.piedrazul.backend.agenda.internal.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidadorCita {

    private final List<ReglaCita> reglas;

    public ValidadorCita(List<ReglaCita> reglas) {
        this.reglas = reglas;
    }

    public void validar(ContextoValidacionCita contexto) {
        reglas.forEach(r -> r.validar(contexto));
    }
}
