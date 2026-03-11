package com.piedrazul.backend.agenda.domain;

import com.piedrazul.backend.auth.domain.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad Médico/Terapista.
 * Especialidades: TERAPIA_NEURAL, QUIROPRAXIA, FISIOTERAPIA
 */
@Entity
@Table(name = "medicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 50)
    private String especialidad;

    @Column(nullable = false, length = 20)
    private String estado;

    @OneToOne(mappedBy = "medico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ConfiguracionMedico configuracion;
}

