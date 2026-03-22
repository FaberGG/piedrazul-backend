package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaDinamicaResponse {

    private LocalDate fecha;
    private String medico;
    private LocalDateTime primerSlotDisponible;
    private List<AgendaDinamicaBloqueResponse> bloques;
}

