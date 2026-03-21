package com.piedrazul.backend.medicos.domain;


import com.piedrazul.backend.auth.domain.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.modulith.NamedInterface;

import java.time.LocalTime;


@Entity(name = "MedicosPaciente")
@Table(name = "medicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamedInterface
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

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
