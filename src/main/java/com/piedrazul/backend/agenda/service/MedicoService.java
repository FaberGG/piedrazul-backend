package com.piedrazul.backend.agenda.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de gestión de médicos y su configuración.
 */
@Service
public class MedicoService {

    /**
     * Lista todos los médicos activos.
     */
    public List<?> listarMedicosActivos() {
        // TODO: consultar médicos con estado ACTIVO
        return List.of();
    }

    /**
     * Lista médicos por especialidad.
     */
    public List<?> listarPorEspecialidad(String especialidad) {
        // TODO: filtrar por especialidad
        return List.of();
    }
}

