package com.piedrazul.backend.agenda.internal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entidad Cita médica.
 * Estados activos: PROGRAMADA, ATENDIDA, CANCELADA
 * Constraint único: (medico_id, fecha, hora) donde estado != CANCELADA
 */
@Entity
@Table(name = "citas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"medico_id", "fecha", "hora"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cita", length = 20)
    private TipoCita tipoCita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoCita.PROGRAMADA;
        if (this.tipoCita == null) this.tipoCita = TipoCita.CONSULTA_GENERAL;
    }
}
