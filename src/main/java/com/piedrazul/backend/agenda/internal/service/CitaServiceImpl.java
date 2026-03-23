package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;
import com.piedrazul.backend.shared.audit.AuditService;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    private static final int HORIZONTE_DIAS_BUSQUEDA = 30;

    private final CitaRepository        citaRepository;
    private final DisponibilidadService disponibilidadService;
    private final PacientesApi          pacientesApi;
    private final MedicosApi            medicosApi;
    private final AuditService          auditService;

    public CitaServiceImpl(CitaRepository citaRepository,
                           DisponibilidadService disponibilidadService,
                           PacientesApi pacientesApi,
                           MedicosApi medicosApi,
                           AuditService auditService) {
        this.citaRepository        = citaRepository;
        this.disponibilidadService = disponibilidadService;
        this.pacientesApi          = pacientesApi;
        this.medicosApi            = medicosApi;
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
        if (!request.getFecha().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha debe ser futura");
        }

        LocalTime hora = parseHora(request.getHora());

        MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(request.getMedicoId());
        if (medico == null) {
            throw new ResourceNotFoundException("Medico", request.getMedicoId());
        }
        if (!medico.isActivo()) {
            throw new BusinessRuleException("El medico no esta activo");
        }

        if (!disponibilidadService.estaDisponible(request.getMedicoId(), request.getFecha(), hora)) {
            throw new BusinessRuleException("El horario seleccionado ya esta ocupado o fuera de la franja de atencion");
        }

        PacienteResumenDTO paciente = pacientesApi.obtenerOCrearPorDocumento(
                RegistroPacienteDTO.builder()
                        .documento(request.getDocumento())
                        .nombres(request.getNombres())
                        .apellidos(request.getApellidos())
                        .celular(request.getCelular())
                        .genero(request.getGenero())
                        .fechaNacimiento(request.getFechaNacimiento())
                        .correo(request.getCorreo())
                        .build()
        );

        Cita cita = Cita.builder()
                .pacienteId(paciente.getId())
                .medicoId(request.getMedicoId())
                .fecha(request.getFecha())
                .hora(hora)
                .estado("PROGRAMADA")
                .observaciones(request.getObservaciones())
                .creadoPor(obtenerUsuarioIdAutenticado())
                .build();

        Cita guardada = citaRepository.save(cita);

        auditService.registrar(
                guardada.getCreadoPor(),
                "CREAR",
                "CITA",
                guardada.getId(),
                "{\"medicoId\":" + guardada.getMedicoId() + ",\"pacienteId\":" + guardada.getPacienteId() + "}",
                "N/A"
        );

        return mapToResponse(guardada, paciente, medico);
    }

    @Override
    @Transactional(readOnly = true)
    public PrimerHorarioDisponibleResponse obtenerPrimerHorarioDisponibleMedico(Long medicoId, LocalDate desde) {
        MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(medicoId);
        if (medico == null) {
            throw new ResourceNotFoundException("Medico", medicoId);
        }
        if (!medico.isActivo()) {
            throw new BusinessRuleException("El medico no esta activo");
        }

        LocalDate fechaInicio = normalizarFechaInicio(desde);

        SlotDisponible slot = buscarPrimerSlotParaMedico(medico, fechaInicio)
                .orElseThrow(() -> new BusinessRuleException("No hay horarios disponibles para el medico en la ventana de busqueda"));

        return mapToPrimerHorarioResponse(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public PrimerHorarioDisponibleResponse obtenerPrimerHorarioDisponibleGlobal(LocalDate desde) {
        LocalDate fechaInicio = normalizarFechaInicio(desde);

        List<MedicoResumenDTO> medicosActivos = medicosApi.listarMedicosActivos();
        if (medicosActivos.isEmpty()) {
            throw new BusinessRuleException("No hay medicos activos para agendar");
        }

        SlotDisponible slot = medicosActivos.stream()
                .map(medico -> buscarPrimerSlotParaMedico(medico, fechaInicio))
                .flatMap(Optional::stream)
                .min(Comparator
                        .comparing(SlotDisponible::fecha)
                        .thenComparing(SlotDisponible::hora)
                        .thenComparing(slotDisponible -> slotDisponible.medico().getId()))
                .orElseThrow(() -> new BusinessRuleException("No hay horarios disponibles en la ventana de busqueda"));

        return mapToPrimerHorarioResponse(slot);
    }

    // ─────────────────────────────────────────────────────────────
    // RF3 — Agendamiento autónomo (paciente)
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
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

        Long usuarioId = obtenerUsuarioIdAutenticado();
        PacienteResumenDTO pacienteResumenDTO = pacientesApi.buscarPorUsuarioId(usuarioId);

        long citasFuturas = citaRepository.countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(pacienteResumenDTO.getId(), "CANCELADA", LocalDate.now());
        if (citasFuturas >= 3) {
            throw new BusinessRuleException("Límite de 3 citas alcanzado");
        }

        MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(request.getMedicoId());
        if (medico == null) {
            throw new ResourceNotFoundException("Medico", request.getMedicoId());
        }
        if (!medico.isActivo()) {
            throw new BusinessRuleException("El medico no esta activo");
        }

        boolean disponibilidad = disponibilidadService.estaDisponible(medico.getId(), request.getFecha(), request.getHora());
        if(!disponibilidad) {
            throw new BusinessRuleException("Horario no disponible");
        }

        Cita cita = Cita.builder()
                .pacienteId(pacienteResumenDTO.getId())
                .medicoId(request.getMedicoId())
                .fecha(request.getFecha())
                .hora(request.getHora())
                .estado("PROGRAMADA")
                .observaciones(request.getObservaciones())
                .creadoPor(usuarioId)
                .build();

        Cita guardada = citaRepository.save(cita);

        auditService.registrar(
                guardada.getCreadoPor(),
                "CREAR",
                "CITA",
                guardada.getId(),
                "{\"medicoId\":" + guardada.getMedicoId() + ",\"pacienteId\":" + guardada.getPacienteId() + "}",
                "N/A"
        );

        return mapToResponse(guardada, paciente, medico);
    }

    private LocalTime parseHora(String hora) {
        try {
            return LocalTime.parse(hora);
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException("La hora debe tener formato HH:mm:ss");
        }
    }

    private Long obtenerUsuarioIdAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Integer userId) {
            return userId.longValue();
        }
        if (principal instanceof String principalStr && principalStr.matches("\\d+")) {
            return Long.parseLong(principalStr);
        }
        return null;
    }

    private CitaResponse mapToResponse(Cita cita, PacienteResumenDTO paciente, MedicoResumenDTO medico) {
        return CitaResponse.builder()
                .id(cita.getId())
                .pacienteNombre(paciente.getNombres() + " " + paciente.getApellidos())
                .pacienteDocumento(paciente.getDocumento())
                .medicoNombre(medico.getNombresCompletos())
                .especialidad(medico.getEspecialidad())
                .fecha(cita.getFecha())
                .hora(cita.getHora())
                .estado(cita.getEstado())
                .observaciones(cita.getObservaciones())
                .build();
    }

    private LocalDate normalizarFechaInicio(LocalDate desde) {
        LocalDate hoy = LocalDate.now();
        if (desde == null || desde.isBefore(hoy)) {
            return hoy;
        }
        return desde;
    }

    private Optional<SlotDisponible> buscarPrimerSlotParaMedico(MedicoResumenDTO medico, LocalDate fechaInicio) {
        HorarioAtencionDTO horario = medicosApi.obtenerHorarioAtencion(medico.getId());
        Integer intervaloMinutos = horario != null ? horario.getIntervaloMinutos() : null;

        for (int i = 0; i <= HORIZONTE_DIAS_BUSQUEDA; i++) {
            LocalDate fecha = fechaInicio.plusDays(i);

            Optional<LocalTime> primeraHoraDisponible = disponibilidadService
                    .calcularHorariosDisponibles(medico.getId(), fecha)
                    .stream()
                    .filter(hora -> !fecha.equals(LocalDate.now()) || hora.isAfter(LocalTime.now()))
                    .sorted()
                    .findFirst();

            if (primeraHoraDisponible.isPresent()) {
                return Optional.of(new SlotDisponible(medico, fecha, primeraHoraDisponible.get(), intervaloMinutos));
            }
        }

        return Optional.empty();
    }

    private PrimerHorarioDisponibleResponse mapToPrimerHorarioResponse(SlotDisponible slot) {
        return PrimerHorarioDisponibleResponse.builder()
                .medicoId(slot.medico().getId())
                .medicoNombre(slot.medico().getNombresCompletos())
                .especialidad(slot.medico().getEspecialidad())
                .fecha(slot.fecha())
                .hora(slot.hora())
                .intervaloMinutos(slot.intervaloMinutos())
                .build();
    }

    private record SlotDisponible(
            MedicoResumenDTO medico,
            LocalDate fecha,
            LocalTime hora,
            Integer intervaloMinutos
    ) {}
}

