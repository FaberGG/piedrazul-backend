package com.piedrazul.backend.agenda.service;

import com.piedrazul.backend.agenda.repository.CitaRepository;
import com.piedrazul.backend.agenda.repository.MedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementación del servicio de disponibilidad horaria (módulo AGENDA).
 *
 * DEPENDENCIAS:
 *  - medicoRepository  → obtener ConfiguracionMedico (horaInicio, horaFin, intervalo, días)
 *  - citaRepository    → consultar citas existentes para filtrar slots ocupados
 *
 * ALGORITMO de calcularHorariosDisponibles:
 *  1. Cargar ConfiguracionMedico del médico
 *  2. Verificar que la fecha solicitada sea un día de atención (diasAtencion)
 *  3. Generar slots: desde horaInicio, sumar intervaloMinutos hasta horaFin
 *     → resultado: [08:00, 08:30, 09:00, ..., 16:30]
 *  4. Cargar citas del día no canceladas: citaRepository.findByMedicoIdAndFecha(...)
 *     → extraer sus horas en un Set<LocalTime>
 *  5. Retornar slots que NO estén en el set de horas ocupadas
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final MedicoRepository medicoRepository;
    private final CitaRepository   citaRepository;

    public DisponibilidadServiceImpl(MedicoRepository medicoRepository,
                                     CitaRepository citaRepository) {
        this.medicoRepository = medicoRepository;
        this.citaRepository   = citaRepository;
    }

    /**
     * {@inheritDoc}
     *
     * Ver algoritmo en el Javadoc de clase.
     * Nota: si el médico no tiene ConfiguracionMedico → retornar List.of()
     *       (no lanzar excepción: puede ser un médico recién creado).
     */
    @Override
    public List<LocalTime> calcularHorariosDisponibles(Long medicoId, LocalDate fecha) {
        // TODO: implementar algoritmo descrito en el Javadoc de clase
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTAR (versión optimizada):
     *  citaRepository.findByMedicoIdAndFecha(medicoId, fecha)
     *  → buscar si hay alguna cita con hora == hora && estado != "CANCELADA"
     *  → si hay → false; si no hay → verificar también que hora esté en los slots del médico
     */
    @Override
    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        // TODO: implementar verificación optimizada (sin calcular todos los slots)
        return false;
    }
}

