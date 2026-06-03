package com.piedrazul.backend.agenda.internal.domain;

public enum TipoCita {
    CONSULTA_GENERAL(false),
    TERAPIA_NEURAL  (true),
    QUIROPRAXIA     (true),
    FISIOTERAPIA    (true),
    ESTANDAR        (false),
    PRIORIDAD       (false);

    private final boolean requiereConsultaPrevia;

    TipoCita(boolean requiereConsultaPrevia) {
        this.requiereConsultaPrevia = requiereConsultaPrevia;
    }

    public boolean requiereConsultaPrevia() {
        return requiereConsultaPrevia;
    }
}