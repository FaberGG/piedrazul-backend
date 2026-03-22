package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaDinamicaBloqueResponse {

    private String rango;
    private boolean estaExpandido;
    private List<AgendaDinamicaSlotResponse> slots;
}

