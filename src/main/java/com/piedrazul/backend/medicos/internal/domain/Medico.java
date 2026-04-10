package com.piedrazul.backend.medicos.internal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity(name = "MedicosPaciente")
@Table(name = "medicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 50)
    private String especialidad;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "hora_inicio_atencion")
    private LocalTime horaInicioAtencion;

    @Column(name = "hora_fin_atencion")
    private LocalTime horaFinAtencion;

    @Column(name = "intervalo_minutos")
    private Integer intervaloMinutos;

    @Column(name = "dias_atencion", length = 128)
    private String diasAtencion;
}