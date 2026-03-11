package com.piedrazul.backend.agenda.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Configuración de horarios y atención de un médico.
 * Relación 1:1 con Medico.
 */
@Entity
@Table(name = "configuracion_medico")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false, unique = true)
    private Medico medico;

    @Column(name = "dias_atencion", columnDefinition = "TEXT")
    private String diasAtencion; // JSON: ["LUNES","MARTES",...]

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "intervalo_minutos", nullable = false)
    private Integer intervaloMinutos;
}

