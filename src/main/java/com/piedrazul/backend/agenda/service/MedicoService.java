package com.piedrazul.backend.agenda.service;

import com.piedrazul.backend.agenda.dto.MedicoResponse;

import java.util.List;

/**
 * Contrato interno del servicio de médicos (módulo AGENDA).
 *
 * Implementación: {@link MedicoServiceImpl}
 */
public interface MedicoService {

    /**
     * Lista todos los médicos con estado ACTIVO.
     *
     * @return lista de médicos activos con su configuración de horario
     */
    List<MedicoResponse> listarMedicosActivos();

    /**
     * Lista médicos activos filtrados por especialidad.
     *
     * @param especialidad TERAPIA_NEURAL | QUIROPRAXIA | FISIOTERAPIA
     * @return lista filtrada; vacía si no hay coincidencias
     */
    List<MedicoResponse> listarPorEspecialidad(String especialidad);

    /**
     * Obtiene los datos de un médico por su ID.
     *
     * @param id ID del médico
     * @return datos del médico
     * @throws com.piedrazul.backend.shared.exception.ResourceNotFoundException si no existe
     */
    MedicoResponse obtenerPorId(Long id);
}



