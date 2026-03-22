package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaBloqueResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaSlotResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaPrioritariaRequest;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final int DURACION_MINIMA_ATENCION_MINUTOS = 15;
    private static final int DURACION_PRIORIDAD_MINUTOS = 5;
    private static final DateTimeFormatter HORA_PANEL_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

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

        HorarioAtencionDTO horario = medicosApi.obtenerHorarioAtencion(request.getMedicoId());
        if (horario == null || !horario.isActivo()) {
            throw new BusinessRuleException("El medico no tiene configuracion horaria activa");
        }
        validarHoraSegunConfiguracion(hora, request.getFecha(), horario);

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
                .duracionMinutos(obtenerDuracionEstandar(request.getMedicoId()))
                .tipoCita("ESTANDAR")
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

    @Override
    @Transactional(readOnly = true)
    public AgendaDinamicaResponse obtenerAgendaDinamica(Long medicoId, LocalDate fecha) {
        MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(medicoId);
        if (medico == null) {
            throw new ResourceNotFoundException("Medico", medicoId);
        }
        if (!medico.isActivo()) {
            throw new BusinessRuleException("El medico no esta activo");
        }

        HorarioAtencionDTO horario = medicosApi.obtenerHorarioAtencion(medicoId);
        if (horario == null || !horario.isActivo()) {
            throw new BusinessRuleException("El medico no tiene configuracion horaria activa");
        }

        List<Cita> citasDia = citaRepository.findByMedicoIdAndFecha(medicoId, fecha)
                .stream()
                .filter(cita -> !"CANCELADA".equalsIgnoreCase(cita.getEstado()))
                .sorted(Comparator.comparing(Cita::getHora))
                .toList();

        Map<Long, PacienteResumenDTO> pacientesPorId = new HashMap<>();
        for (Cita cita : citasDia) {
            pacientesPorId.computeIfAbsent(cita.getPacienteId(), pacientesApi::obtenerResumenPorId);
        }

        List<SlotPanel> slotsPanel = construirSlotsPanelReducido(horario, citasDia, medicoId);
        LocalDateTime primerSlotDisponible = calcularPrimerSlotDisponible(fecha, slotsPanel);

        List<AgendaDinamicaBloqueResponse> bloques = construirBloquesAgenda(
                horario,
                fecha,
                slotsPanel,
                citasDia,
                pacientesPorId,
                primerSlotDisponible
        );

        return AgendaDinamicaResponse.builder()
                .fecha(fecha)
                .medico(medico.getNombresCompletos())
                .primerSlotDisponible(primerSlotDisponible)
                .bloques(bloques)
                .build();
    }

    @Override
    public CitaResponse crearCitaPrioritaria(CrearCitaPrioritariaRequest request) {
        MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(request.getMedicoId());
        if (medico == null) {
            throw new ResourceNotFoundException("Medico", request.getMedicoId());
        }
        if (!medico.isActivo()) {
            throw new BusinessRuleException("El medico no esta activo");
        }

        HorarioAtencionDTO horario = medicosApi.obtenerHorarioAtencion(request.getMedicoId());
        if (horario == null || !horario.isActivo()) {
            throw new BusinessRuleException("El medico no tiene configuracion horaria activa");
        }

        LocalTime horaReferencia = parseHora(request.getHoraReferencia());

        List<Cita> citasDia = citaRepository.findByMedicoIdAndFecha(request.getMedicoId(), request.getFecha())
                .stream()
                .filter(cita -> !"CANCELADA".equalsIgnoreCase(cita.getEstado()))
                .sorted(Comparator.comparing(Cita::getHora))
                .toList();

        Cita citaBase = citasDia.stream()
                .filter(cita -> cita.getHora().equals(horaReferencia))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No existe una cita de referencia en la hora indicada"));

        if ("PRIORIDAD".equalsIgnoreCase(citaBase.getTipoCita())) {
            throw new BusinessRuleException("La cita de referencia ya es prioritaria");
        }

        Cita citaSiguiente = citasDia.stream()
                .filter(cita -> cita.getHora().isAfter(citaBase.getHora()))
                .findFirst()
                .orElse(null);

        LocalTime finBaseOriginal = finCita(citaBase, request.getMedicoId());
        LocalTime limiteSiguiente = citaSiguiente != null ? citaSiguiente.getHora() : horario.getHoraFin();

        if (!finBaseOriginal.isBefore(limiteSiguiente) && !finBaseOriginal.equals(limiteSiguiente)) {
            throw new BusinessRuleException("La agenda actual no permite insertar sobrecupo despues de la cita seleccionada");
        }

        long huecoActual = minutosEntre(finBaseOriginal, limiteSiguiente);
        int flexBase = Math.max(0, duracionCita(citaBase, request.getMedicoId()) - DURACION_MINIMA_ATENCION_MINUTOS);
        int flexSiguiente = citaSiguiente != null
                ? Math.max(0, duracionCita(citaSiguiente, request.getMedicoId()) - DURACION_MINIMA_ATENCION_MINUTOS)
                : 0;

        if (huecoActual + flexBase + flexSiguiente < DURACION_PRIORIDAD_MINUTOS) {
            throw new BusinessRuleException("No hay flexibilidad suficiente para crear una cita prioritaria de 5 minutos");
        }

        LocalTime inicioPrioridad = citaBase.getHora().plusMinutes(DURACION_MINIMA_ATENCION_MINUTOS);
        if (citaSiguiente != null && !inicioPrioridad.plusMinutes(DURACION_PRIORIDAD_MINUTOS).isBefore(citaSiguiente.getHora())
                && !inicioPrioridad.plusMinutes(DURACION_PRIORIDAD_MINUTOS).equals(citaSiguiente.getHora())) {
            throw new BusinessRuleException("No hay espacio inmediato para insertar la cita prioritaria");
        }

        if (citaRepository.existsByMedicoIdAndFechaAndHoraAndEstadoNot(
                request.getMedicoId(), request.getFecha(), inicioPrioridad, "CANCELADA")) {
            throw new BusinessRuleException("El horario prioritario ya se encuentra ocupado");
        }

        citaBase.setDuracionMinutos(DURACION_MINIMA_ATENCION_MINUTOS);
        citaRepository.save(citaBase);

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

        Cita prioridad = Cita.builder()
                .pacienteId(paciente.getId())
                .medicoId(request.getMedicoId())
                .fecha(request.getFecha())
                .hora(inicioPrioridad)
                .duracionMinutos(DURACION_PRIORIDAD_MINUTOS)
                .tipoCita("PRIORIDAD")
                .estado("PROGRAMADA")
                .observaciones(request.getObservaciones())
                .creadoPor(obtenerUsuarioIdAutenticado())
                .build();

        Cita guardada = citaRepository.save(prioridad);

        auditService.registrar(
                guardada.getCreadoPor(),
                "CREAR_PRIORIDAD",
                "CITA",
                guardada.getId(),
                "{\"medicoId\":" + guardada.getMedicoId() + ",\"pacienteId\":" + guardada.getPacienteId() + "}",
                "N/A"
        );

        return mapToResponse(guardada, paciente, medico);
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

    private int obtenerDuracionEstandar(Long medicoId) {
        HorarioAtencionDTO horario = medicosApi.obtenerHorarioAtencion(medicoId);
        return horario != null ? horario.getIntervaloMinutos() : 15;
    }

    private int duracionCita(Cita cita, Long medicoId) {
        if (cita.getDuracionMinutos() != null && cita.getDuracionMinutos() > 0) {
            return cita.getDuracionMinutos();
        }
        return obtenerDuracionEstandar(medicoId);
    }

    private LocalTime finCita(Cita cita, Long medicoId) {
        return cita.getHora().plusMinutes(duracionCita(cita, medicoId));
    }

    private long minutosEntre(LocalTime inicio, LocalTime fin) {
        return java.time.Duration.between(inicio, fin).toMinutes();
    }

    private LocalDateTime calcularPrimerSlotDisponible(LocalDate fecha, List<SlotPanel> slotsPanel) {
        LocalTime horaLimite = fecha.equals(LocalDate.now()) ? LocalTime.now() : LocalTime.MIN;

        return slotsPanel.stream()
                .filter(slot -> slot.cita() == null)
                .map(SlotPanel::hora)
                .filter(slot -> !slot.isBefore(horaLimite))
                .findFirst()
                .map(slot -> LocalDateTime.of(fecha, slot))
                .orElse(null);
    }

    private List<SlotPanel> construirSlotsPanelReducido(HorarioAtencionDTO horario, List<Cita> citasDia, Long medicoId) {
        List<SlotPanel> slots = new java.util.ArrayList<>();
        LocalTime cursor = horario.getHoraInicio();

        for (Cita cita : citasDia) {
            if (cita.getHora().isAfter(cursor)) {
                // Si hay hueco, se expone solo el primer minuto accionable de ese tramo.
                slots.add(new SlotPanel(cursor, null));
            }

            slots.add(new SlotPanel(cita.getHora(), cita));

            LocalTime fin = finCita(cita, medicoId);
            if (fin.isAfter(cursor)) {
                cursor = fin;
            }
        }

        if (cursor.isBefore(horario.getHoraFin())) {
            slots.add(new SlotPanel(cursor, null));
        }

        return slots;
    }

    private List<AgendaDinamicaBloqueResponse> construirBloquesAgenda(
            HorarioAtencionDTO horario,
            LocalDate fecha,
            List<SlotPanel> slotsPanel,
            List<Cita> citasDia,
            Map<Long, PacienteResumenDTO> pacientesPorId,
            LocalDateTime primerSlotDisponible
    ) {
        Map<Integer, List<SlotPanel>> slotsPorBloque = new java.util.LinkedHashMap<>();
        for (SlotPanel slot : slotsPanel) {
            int clave = slot.hora().getHour();
            slotsPorBloque.computeIfAbsent(clave, ignored -> new java.util.ArrayList<>()).add(slot);
        }

        return slotsPorBloque.entrySet().stream()
                .map(entry -> {
                    List<AgendaDinamicaSlotResponse> slots = entry.getValue().stream()
                            .map(slot -> mapSlotAgenda(slot, citasDia, pacientesPorId, horario))
                            .toList();

                    LocalTime inicioBloque = entry.getValue().get(0).hora();
                    LocalTime finBloque = inicioBloque.plusHours(1);

                    boolean expandido = primerSlotDisponible != null
                            && primerSlotDisponible.toLocalDate().equals(fecha)
                            && !primerSlotDisponible.toLocalTime().isBefore(inicioBloque)
                            && primerSlotDisponible.toLocalTime().isBefore(finBloque);

                    return AgendaDinamicaBloqueResponse.builder()
                            .rango(inicioBloque.format(HORA_PANEL_FORMAT) + " - " + finBloque.format(HORA_PANEL_FORMAT))
                            .estaExpandido(expandido)
                            .slots(slots)
                            .build();
                })
                .toList();
    }

    private AgendaDinamicaSlotResponse mapSlotAgenda(
            SlotPanel slot,
            List<Cita> citasDia,
            Map<Long, PacienteResumenDTO> pacientesPorId,
            HorarioAtencionDTO horario
    ) {
        if (slot.cita() == null) {
            return AgendaDinamicaSlotResponse.builder()
                    .hora(slot.hora().format(HORA_PANEL_FORMAT))
                    .estado("LIBRE")
                    .permiteAbrirPrioridadPosterior(false)
                    .build();
        }

        Cita cita = slot.cita();
        PacienteResumenDTO paciente = pacientesPorId.get(cita.getPacienteId());

        boolean permitePrioridad = cita.getHora().equals(slot.hora())
                && !"PRIORIDAD".equalsIgnoreCase(cita.getTipoCita())
                && puedeAbrirPrioridadPosterior(citasDia, cita, horario);

        return AgendaDinamicaSlotResponse.builder()
                .hora(slot.hora().format(HORA_PANEL_FORMAT))
                .estado("OCUPADO")
                .citaId(cita.getId())
                .pacienteDocumento(paciente != null ? paciente.getDocumento() : null)
                .pacienteNombres(paciente != null ? paciente.getNombres() : null)
                .pacienteApellidos(paciente != null ? paciente.getApellidos() : null)
                .pacienteCelular(paciente != null ? paciente.getCelular() : null)
                .permiteAbrirPrioridadPosterior(permitePrioridad)
                .build();
    }

    private boolean puedeAbrirPrioridadPosterior(List<Cita> citasDia, Cita citaActual, HorarioAtencionDTO horario) {
        Cita siguiente = citasDia.stream()
                .filter(cita -> cita.getHora().isAfter(citaActual.getHora()))
                .findFirst()
                .orElse(null);

        LocalTime finActual = finCita(citaActual, citaActual.getMedicoId());
        LocalTime limite = siguiente != null ? siguiente.getHora() : horario.getHoraFin();

        if (limite.isBefore(finActual)) {
            return false;
        }

        long hueco = minutosEntre(finActual, limite);
        int flexActual = Math.max(0, duracionCita(citaActual, citaActual.getMedicoId()) - DURACION_MINIMA_ATENCION_MINUTOS);
        int flexSiguiente = siguiente != null
                ? Math.max(0, duracionCita(siguiente, siguiente.getMedicoId()) - DURACION_MINIMA_ATENCION_MINUTOS)
                : 0;

        boolean prioridadIntermedia = citasDia.stream().anyMatch(cita ->
                "PRIORIDAD".equalsIgnoreCase(cita.getTipoCita())
                        && cita.getHora().isAfter(citaActual.getHora())
                        && cita.getHora().isBefore(limite)
        );

        return !prioridadIntermedia && (hueco + flexActual + flexSiguiente) >= DURACION_PRIORIDAD_MINUTOS;
    }

    private void validarHoraSegunConfiguracion(LocalTime hora, LocalDate fecha, HorarioAtencionDTO horario) {
        if (!horario.getDiasAtencion().contains(fecha.getDayOfWeek())) {
            throw new BusinessRuleException("El medico no atiende en la fecha seleccionada");
        }

        if (hora.isBefore(horario.getHoraInicio()) || !hora.isBefore(horario.getHoraFin())) {
            throw new BusinessRuleException("La hora esta fuera de la franja de atencion del medico");
        }

        long minutosDesdeInicio = java.time.Duration.between(horario.getHoraInicio(), hora).toMinutes();
        if (minutosDesdeInicio % horario.getIntervaloMinutos() != 0) {
            throw new BusinessRuleException("La hora debe respetar el intervalo configurado del medico (" + horario.getIntervaloMinutos() + " minutos)");
        }
    }

    private record SlotPanel(LocalTime hora, Cita cita) {}
}

