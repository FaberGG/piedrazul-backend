package com.piedrazul.backend.agenda.internal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuración global del sistema (tabla clave-valor).
 * Ej: VENTANA_AGENDAMIENTO_SEMANAS, LIMITE_CITAS_FUTURAS_PACIENTE, etc.
 */
@Entity
@Table(name = "configuracion_global")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(length = 255)
    private String descripcion;
}

