package com.piedrazul.backend.agenda.internal.domain;

public enum EspecialidadMedica {
    MEDICINA_GENERAL(TipoCita.CONSULTA_GENERAL),
    TERAPIA_NEURAL  (TipoCita.TERAPIA_NEURAL),
    QUIROPRAXIA     (TipoCita.QUIROPRAXIA),
    FISIOTERAPIA    (TipoCita.FISIOTERAPIA),
    ESTANDAR        (TipoCita.ESTANDAR);

    private final TipoCita tipoCita;

    EspecialidadMedica(TipoCita tipoCita) {
        this.tipoCita = tipoCita;
    }

    public TipoCita getTipoCita() {
        return tipoCita;
    }

    public static EspecialidadMedica fromString(String value) {
        if (value == null) return MEDICINA_GENERAL;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ESTANDAR;
        }
    }
}
