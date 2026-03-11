package com.piedrazul.backend.agenda.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Historial de cambios (reagendamientos) de una cita.
 * Registra fecha/hora/médico anterior y nuevo, motivo y usuario que modificó.
 */
@Entity
@Table(name = "historial_cambios_cita")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCambiosCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(name = "fecha_anterior")
    private LocalDate fechaAnterior;

    @Column(name = "hora_anterior")
    private LocalTime horaAnterior;

    @Column(name = "medico_anterior_id")
    private Long medicoAnteriorId;

    @Column(name = "fecha_nueva")
    private LocalDate fechaNueva;

    @Column(name = "hora_nueva")
    private LocalTime horaNueva;

    @Column(name = "medico_nuevo_id")
    private Long medicoNuevoId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "modificado_por", nullable = false)
    private Long modificadoPor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

