package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.shared.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementación del servicio de citas (módulo AGENDA).
 *
 * DEPENDENCIAS NECESARIAS para implementar cada método:
 *  - citaRepository       → persistir y consultar citas
 *  - disponibilidadService → verificar slots libres
 *  - auditService         → registrar operaciones críticas
 *
 * OBTENER USUARIO AUTENTICADO (en métodos que lo necesiten):
 *  Long userId = (Long) SecurityContextHolder.getContext()
 *                        .getAuthentication().getPrincipal();
 *  // Requiere configurar CustomUserDetails en JwtAuthFilter.
 */
@Service
@Transactional
public class CitaServiceImpl implements CitaService {

    private final CitaRepository        citaRepository;
    private final DisponibilidadService disponibilidadService;
    private final AuditService          auditService;

    public CitaServiceImpl(CitaRepository citaRepository,
                           DisponibilidadService disponibilidadService,
                           AuditService auditService) {
        this.citaRepository        = citaRepository;
        this.disponibilidadService = disponibilidadService;
        this.auditService          = auditService;
    }

    // ─────────────────────────────────────────────────────────────
    // RF1 — Listar agenda de un médico por fecha
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * FLUJO MODULAR - PASOS A IMPLEMENTAR:
     * 1. Validación de Identidad y Configuración (Comunicación con Médicos):
     * - Llamar a medicosApi.obtenerConfiguracionAgenda(medicoId).
     * - Este DTO debe incluir: horaInicio, horaFin, intervaloMinutos y si el médico está activo.
     * -> Si no existe o está inactivo: throw ResourceNotFoundException o BusinessRuleException.
     * 2. Obtención de Citas (Interno Agenda):
     * - List<Cita> citas = citaRepository.findByMedicoIdAndFecha(medicoId, fecha).
     * - Nota: Esta lista solo tiene IDs de pacientes.
     * 3. Enriquecimiento de Datos (Comunicación con Pacientes):
     * - Extraer Set<Long> pacienteIds de la lista de citas.
     * - Llamar a pacientesApi.obtenerNombresResumen(pacienteIds).
     * - Recibir un Map<Long, PacienteResumenDTO> para evitar múltiples llamadas.
     * 4. Cálculo de Disponibilidad y Ocupación (Lógica de Negocio de Agenda):
     * - Generar la lista de todos los 'slots' posibles usando la configuración obtenida en el Paso 1.
     * - Comparar slots generados vs citas existentes para marcar cuáles están ocupados.
     * - Calcular: porcentajeOcupacion = (citas.size() / totalSlotsPosibles) * 100.
     * 5. Mapeo y Construcción de Respuesta:
     * - Transformar cada Cita en CitaResponse, inyectando el nombre del paciente desde el Map del Paso 3.
     * - Construir AgendaResponse con la lista de slots (ocupados/libres) y métricas de ocupación.
     */
    @Override
    @Transactional(readOnly = true)
    public AgendaResponse listarAgendaMedico(Long medicoId, LocalDate fecha) {
        throw new UnsupportedOperationException("TODO RF1: implementar listarAgendaMedico");
    }

    // ─────────────────────────────────────────────────────────────
    // RF2 — Crear cita manual
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     * PASOS A IMPLEMENTAR:
     * 1. comunicacion modulo Pacientes
     * 2. verificar disponibilidad con disponibilidadService.estaDisponible(request.medicoId, request.fecha, request.hora)
     * 3. crear y guardar cita con citaRepository.save(cita)
     * 4. registrar operación en auditService.registrar(...)
     */
    @Override
    public CitaResponse crearCitaManual(CrearCitaManualRequest request) {
        throw new UnsupportedOperationException("TODO RF2: implementar crearCitaManual");
    }

    // ─────────────────────────────────────────────────────────────
    // RF3 — Agendamiento autónomo (paciente)
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * FLUJO MODULAR - PASOS A IMPLEMENTAR:
     * 1. Identificación del Paciente:
     * - Obtener el 'usuarioId' del SecurityContextHolder.
     * - Llamar a pacientesApi.buscarIdPorUsuarioId(usuarioId).
     * -> Si no existe: throw EntityNotFoundException("El usuario no tiene un perfil de paciente asociado").
     * 2. Validación de Reglas de Negocio (Internas de Agenda):
     * - Consultar citaRepository.countByPacienteIdAndEstadoAndFechaPositiva(...)
     * -> Si >= 3: throw BusinessRuleException("Límite de 3 citas futuras alcanzado").
     * 3. Validación de Disponibilidad (Comunicación con Médicos):
     * - Llamar a medicosApi.verificarHabilitacionParaCita(request.medicoId, request.fecha, request.hora).
     * - Esta llamada interna valida: estado ACTIVO del médico, franja horaria y feriados.
     * -> Si retorna false: throw BusinessRuleException("El médico no está disponible en el horario seleccionado").
     * 4. Validación de Cruce de Horarios (Interna de Agenda):
     * - verificarDisponibilidadInterna(request.medicoId, request.fecha, request.hora).
     * - Comprobar que no exista otra Cita en ese slot exacto para ese medicoId.
     * 5. Persistencia (Desacoplada):
     * - Crear entidad Cita usando solo pacienteId (Long) y medicoId (Long).
     * - Usar @Lock(PESSIMISTIC_WRITE) en la consulta de validación previa para evitar Race Conditions.
     * - citaRepository.save(cita).
     * 6. Notificación y Auditoría (Asíncrona/Eventos):
     * - Publicar evento interno: CitaProgramadaEvent(citaId, pacienteId, medicoId).
     * - Los módulos de Auditoría y Notificaciones reaccionarán de forma independiente.
     * 7. Retornar CitaResponse (Mapeado desde la entidad).
     */
    @Override
    public CitaResponse agendarAutonomo(AgendarAutonomoRequest request) {
        throw new UnsupportedOperationException("TODO RF3: implementar agendarAutonomo");
    }
}

