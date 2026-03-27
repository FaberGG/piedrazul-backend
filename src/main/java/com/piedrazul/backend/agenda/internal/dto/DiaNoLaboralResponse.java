package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaboralResponse {
    private Long id;
    private LocalDate fecha;
    private String descripcion;
}