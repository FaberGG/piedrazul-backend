package com.piedrazul.backend.agenda.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReagendarCitaRequest {

    @NotNull(message = "La nueva fecha es obligatoria")
    private LocalDate nuevaFecha;

    @NotBlank(message = "La nueva hora es obligatoria")
    private String nuevaHora;

    @NotBlank(message = "El motivo del reagendamiento es obligatorio")
    private String motivo;

    /** Opcional: si es null se conserva el mismo médico de la cita original. */
    private Long medicoNuevoId;
}
