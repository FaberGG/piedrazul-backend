package com.piedrazul.backend.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * DTO de respuesta para un médico/terapista.
 * Incluye datos básicos y, opcionalmente, su configuración de horario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoResponse {

    private Long id;
    private String nombres;
    private String apellidos;
    /** TERAPIA_NEURAL | QUIROPRAXIA | FISIOTERAPIA */
    private String especialidad;
    /** MEDICO | TERAPISTA */
    private String tipo;
    /** ACTIVO | INACTIVO */
    private String estado;

    /**
     * Configuración de horario del médico.
     * Puede ser {@code null} si el médico aún no tiene horario configurado.
     */
    private HorarioResponse horario;

    /**
     * DTO anidado que representa la configuración de atención de un médico
     * (mapeado desde {@link com.piedrazul.backend.agenda.domain.ConfiguracionMedico}).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HorarioResponse {
        /** Días de atención, p. ej. ["LUNES", "MARTES", "MIERCOLES"]. */
        private List<String> diasAtencion;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private Integer intervaloMinutos;
    }
}

