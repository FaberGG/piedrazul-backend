package com.piedrazul.backend.agenda.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionAgendaRequest {

    @NotNull(message = "La ventana de agendamiento es obligatoria")
    @Min(value = 1, message = "Mínimo 1 semana")
    @Max(value = 12, message = "Máximo 12 semanas")
    private Integer ventanaAgendamientoSemanas;
}