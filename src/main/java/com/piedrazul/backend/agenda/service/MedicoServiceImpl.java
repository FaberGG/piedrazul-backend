package com.piedrazul.backend.agenda.service;

import com.piedrazul.backend.agenda.dto.MedicoResponse;
import com.piedrazul.backend.agenda.repository.MedicoRepository;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de médicos (módulo AGENDA).
 *
 * DEPENDENCIAS:
 *  - medicoRepository → consultar médicos y su ConfiguracionMedico
 *
 * MAPEO Medico → MedicoResponse:
 *  Al implementar, mapear también medico.configuracion → MedicoResponse.HorarioResponse.
 *  Si configuracion es null (médico sin configurar), horario puede ser null.
 */
@Service
@Transactional(readOnly = true)
public class MedicoServiceImpl implements MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoServiceImpl(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTAR:
     *  medicoRepository.findByEstado("ACTIVO")
     *  → mapear cada Medico → MedicoResponse (incluir horario si tiene ConfiguracionMedico)
     */
    @Override
    public List<MedicoResponse> listarMedicosActivos() {
        // TODO: medicoRepository.findByEstado("ACTIVO") → mapear a MedicoResponse
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTAR:
     *  medicoRepository.findByEspecialidad(especialidad)
     *  → filtrar solo los ACTIVOS → mapear a MedicoResponse
     */
    @Override
    public List<MedicoResponse> listarPorEspecialidad(String especialidad) {
        // TODO: medicoRepository.findByEspecialidad(especialidad) → filtrar ACTIVOS → mapear
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTAR:
     *  medicoRepository.findById(id)
     *  → lanzar ResourceNotFoundException si no existe
     *  → mapear a MedicoResponse
     */
    @Override
    public MedicoResponse obtenerPorId(Long id) {
        // TODO: medicoRepository.findById(id).orElseThrow(ResourceNotFoundException)
        throw new ResourceNotFoundException("Médico no encontrado con id: " + id);
    }
}

