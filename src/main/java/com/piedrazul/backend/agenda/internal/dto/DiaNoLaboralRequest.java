package com.piedrazul.backend.agenda.internal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaboralRequest {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String descripcion;
}