package com.piedrazul.backend.agenda.internal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
@Entity
@Table(name = "dias_no_laborales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(length = 150)
    private String descripcion;
}
