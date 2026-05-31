package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;
import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaBloqueResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaSlotResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaPrioritariaRequest;
import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;
import com.piedrazul.backend.agenda.internal.domain.AgendaDiaLock;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.HistorialCambiosCita;
import com.piedrazul.backend.agenda.internal.dto.ActualizarCitaRequest;
import com.piedrazul.backend.agenda.internal.dto.CitaDetalleResponse;
import com.piedrazul.backend.agenda.internal.dto.HistorialCambiosCitaResponse;
import com.piedrazul.backend.agenda.internal.dto.ReagendarCitaRequest;
import com.piedrazul.backend.pacientes.api.dto.ActualizarPacienteDTO;
import com.piedrazul.backend.agenda.internal.event.AgendaDinamicaChangedEvent;
import com.piedrazul.backend.agenda.internal.repository.AgendaDiaLockRepository;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.agenda.internal.repository.HistorialCambiosCitaRepository;
import com.piedrazul.backend.auth.api.AuthApi;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;
import com.piedrazul.backend.shared.audit.AuditService;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.hibernate.AssertionFailure;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
import java.util.UUID;

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
        private static final DateTimeFormatter HORA_AGENDA_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

        private final CitaRepository citaRepository;
        private final AgendaDiaLockRepository agendaDiaLockRepository;
        private final HistorialCambiosCitaRepository historialRepository;
        private final DisponibilidadService disponibilidadService;
        private final PacientesApi pacientesApi;
        private final MedicosApi medicosApi;
        private final AuthApi authApi;
        private final AuditService auditService;
        private final ApplicationEventPublisher eventPublisher;

        public CitaServiceImpl(CitaRepository citaRepository,
                               AgendaDiaLockRepository agendaDiaLockRepository,
                               HistorialCambiosCitaRepository historialRepository,
                               DisponibilidadService disponibilidadService,
                               PacientesApi pacientesApi,
                               MedicosApi medicosApi,
                               AuthApi authApi,
                               AuditService auditService,
                               ApplicationEventPublisher eventPublisher) {
            this.citaRepository = citaRepository;
            this.agendaDiaLockRepository = agendaDiaLockRepository;
            this.historialRepository = historialRepository;
            this.disponibilidadService = disponibilidadService;
            this.pacientesApi = pacientesApi;
            this.medicosApi = medicosApi;
            this.authApi = authApi;
            this.auditService = auditService;
            this.eventPublisher = eventPublisher;
        }

        // ─────────────────────────────────────────────────────────────
        // RF1 — Listar agenda de un médico por fecha
        // ─────────────────────────────────────────────────────────────

        /**
         * {@inheritDoc}
         * <p>
         * Flujo implementado:
         * 1) Valida que el medico exista y este activo usando {@link MedicosApi}.
         * 2) Obtiene la configuracion horaria activa del medico (intervalo, jornada y dias de atencion).
         * 3) Consulta las citas del dia y filtra las canceladas para los calculos de ocupacion.
         * 4) Enriquece cada cita con datos resumidos del paciente usando {@link PacientesApi}.
         * 5) Calcula disponibilidad y metricas ({@code totalSlots}, {@code slotsOcupados},
         * {@code porcentajeOcupacion}) segun configuracion del medico.
         * 6) Retorna un {@link AgendaResponse} listo para consumo de panel de agenda.
         */
        @Override
        @Transactional(readOnly = true)
        public AgendaResponse listarAgendaMedico(Long medicoId, LocalDate fecha) {
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

            List<Cita> citasActivas = citaRepository.findByMedicoIdAndFecha(medicoId, fecha)
                    .stream()
                    .filter(cita -> !"CANCELADA".equalsIgnoreCase(cita.getEstado()))
                    .sorted(Comparator.comparing(Cita::getHora))
                    .toList();

            Map<Long, PacienteResumenDTO> pacientesPorId = new HashMap<>();
            for (Cita cita : citasActivas) {
                pacientesPorId.computeIfAbsent(cita.getPacienteId(), pacientesApi::obtenerResumenPorId);
            }

            List<CitaResponse> citas = citasActivas.stream()
                    .map(cita -> {
                        PacienteResumenDTO paciente = pacientesPorId.get(cita.getPacienteId());
                        String nombrePaciente = paciente != null
                                ? (paciente.getNombres() + " " + paciente.getApellidos()).trim()
                                : "Paciente no encontrado";

                        return CitaResponse.builder()
                                .id(cita.getId())
                                .pacienteNombre(nombrePaciente)
                                .pacienteDocumento(paciente != null ? paciente.getDocumento() : null)
                                .medicoNombre(medico.getNombresCompletos())
                                .especialidad(medico.getEspecialidad())
                                .fecha(cita.getFecha())
                                .hora(cita.getHora())
                                .estado(cita.getEstado())
                                .observaciones(cita.getObservaciones())
                                .build();
                    })
                    .toList();

            List<String> horariosDisponibles = disponibilidadService.calcularHorariosDisponibles(medicoId, fecha)
                    .stream()
                    .sorted()
                    .map(hora -> hora.format(HORA_AGENDA_FORMAT))
                    .toList();

            int totalSlots = calcularTotalSlots(horario, fecha);
            int slotsOcupados = citasActivas.size();
            double porcentajeOcupacion = totalSlots > 0
                    ? (slotsOcupados * 100.0) / totalSlots
                    : 0.0;

            return AgendaResponse.builder()
                    .medicoId(medicoId)
                    .medicoNombre(medico.getNombresCompletos())
                    .especialidad(medico.getEspecialidad())
                    .fecha(fecha)
                    .citas(citas)
                    .horariosDisponibles(horariosDisponibles)
                    .totalSlots(totalSlots)
                    .slotsOcupados(slotsOcupados)
                    .porcentajeOcupacion(porcentajeOcupacion)
                    .build();
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

            try {
                adquirirBloqueoOptimistaAgenda(request.getMedicoId(), request.getFecha());

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

                publicarCambioAgenda(guardada.getMedicoId(), guardada.getFecha(), guardada.getId(), "CITA_MANUAL_CREADA");

                return mapToResponse(guardada, paciente, medico);
            } catch (AgendaLockConcurrencyException | ObjectOptimisticLockingFailureException | AssertionFailure ex) {
                throw conflictoConcurrencia();
            } catch (DataIntegrityViolationException ex) {
                throw conflictoSlotOcupado();
            }
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
            try {
                adquirirBloqueoOptimistaAgenda(request.getMedicoId(), request.getFecha());

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

                publicarCambioAgenda(guardada.getMedicoId(), guardada.getFecha(), guardada.getId(), "CITA_PRIORIDAD_CREADA");

                return mapToResponse(guardada, paciente, medico);
            } catch (AgendaLockConcurrencyException | ObjectOptimisticLockingFailureException | AssertionFailure ex) {
                throw conflictoConcurrencia();
            } catch (DataIntegrityViolationException ex) {
                throw conflictoSlotOcupado();
            }
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
            try {
                adquirirBloqueoOptimistaAgenda(request.getMedicoId(), request.getFecha());

                UUID usuarioId = obtenerUsuarioIdAutenticado();
                if (usuarioId == null) {
                    throw new BusinessRuleException("No fue posible resolver el usuario autenticado");
                }
                PacienteResumenDTO pacienteResumenDTO = pacientesApi.buscarPorUsuarioId(usuarioId);

                // Bloquear si el paciente ya tiene una cita PROGRAMADA futura.
                // ATENDIDA y CANCELADA se consideran resueltas y no bloquean nuevas reservas.
                boolean tieneCitaActiva = citaRepository.existsByPacienteIdAndEstadoInAndFechaGreaterThanEqual(
                        pacienteResumenDTO.getId(),
                        List.of("PROGRAMADA"),
                        LocalDate.now()
                );
                if (tieneCitaActiva) {
                    throw new BusinessRuleException(
                            "Ya tienes una cita programada o confirmada. Cancélala antes de agendar una nueva."
                    );
                }

                long citasFuturas = citaRepository.countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(pacienteResumenDTO.getId(), "CANCELADA", LocalDate.now());
                if (citasFuturas >= 3) {
                    throw new BusinessRuleException("Límite de 3 citas alcanzado");
                }

                MedicoResumenDTO medicoResumenDTO = medicosApi.obtenerResumenMedico(request.getMedicoId());
                if (medicoResumenDTO == null) {
                    throw new ResourceNotFoundException("Medico", request.getMedicoId());
                }
                if (!medicoResumenDTO.isActivo()) {
                    throw new BusinessRuleException("El medico no esta activo");
                }

                boolean disponibilidad = disponibilidadService.estaDisponible(medicoResumenDTO.getId(), request.getFecha(), request.getHora());
                if (!disponibilidad) {
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

                publicarCambioAgenda(guardada.getMedicoId(), guardada.getFecha(), guardada.getId(), "CITA_AUTONOMA_CREADA");

                return mapToResponse(guardada, pacienteResumenDTO, medicoResumenDTO);
            } catch (AgendaLockConcurrencyException | ObjectOptimisticLockingFailureException | AssertionFailure ex) {
                throw conflictoConcurrencia();
            } catch (DataIntegrityViolationException ex) {
                throw conflictoSlotOcupado();
            }
        }

        // ─────────────────────────────────────────────────────────────
        // RF8 — Reagendar cita atendida como seguimiento
        // ─────────────────────────────────────────────────────────────

        @Override
        public CitaResponse reagendarCita(Long citaId, ReagendarCitaRequest request) {
            try {
                Cita cita = citaRepository.findById(citaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Cita", citaId));

                // Solo las citas ya atendidas se pueden reagendar como seguimiento
                if (!"ATENDIDA".equals(cita.getEstado())) {
                    throw new BusinessRuleException("Solo se pueden reagendar citas que ya fueron atendidas");
                }

                Long medicoId = request.getMedicoNuevoId() != null
                        ? request.getMedicoNuevoId()
                        : cita.getMedicoId();

                adquirirBloqueoOptimistaAgenda(medicoId, request.getNuevaFecha());

                LocalTime nuevaHora = parseHora(request.getNuevaHora());

                if (!disponibilidadService.estaDisponible(medicoId, request.getNuevaFecha(), nuevaHora)) {
                    throw new BusinessRuleException("El horario solicitado no esta disponible");
                }

                UUID usuarioId = obtenerUsuarioIdAutenticado();

                // Guardar historial antes de mutar la cita
                historialRepository.save(
                        HistorialCambiosCita.builder()
                                .cita(cita)
                                .fechaAnterior(cita.getFecha())
                                .horaAnterior(cita.getHora())
                                .medicoAnteriorId(cita.getMedicoId())
                                .fechaNueva(request.getNuevaFecha())
                                .horaNueva(nuevaHora)
                                .medicoNuevoId(medicoId)
                                .motivo(request.getMotivo())
                                .modificadoPor(usuarioId)
                                .build()
                );

                LocalDate fechaAnterior = cita.getFecha();

                cita.setFecha(request.getNuevaFecha());
                cita.setHora(nuevaHora);
                cita.setMedicoId(medicoId);
                cita.setEstado("PROGRAMADA");

                Cita guardada = citaRepository.save(cita);

                auditService.registrar(
                        usuarioId,
                        "REPROGRAMAR",
                        "CITA",
                        guardada.getId(),
                        "{\"fechaAnterior\":\"" + fechaAnterior + "\",\"horaNueva\":\"" + nuevaHora + "\",\"motivo\":\"" + request.getMotivo() + "\"}",
                        "N/A"
                );

                // Notificar ambas fechas al panel en tiempo real
                publicarCambioAgenda(guardada.getMedicoId(), fechaAnterior, guardada.getId(), "CITA_REAGENDADA_ORIGEN");
                publicarCambioAgenda(guardada.getMedicoId(), guardada.getFecha(), guardada.getId(), "CITA_REAGENDADA_DESTINO");

                PacienteResumenDTO paciente = pacientesApi.obtenerResumenPorId(guardada.getPacienteId());
                MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(guardada.getMedicoId());

                return mapToResponse(guardada, paciente, medico);

            } catch (AgendaLockConcurrencyException | ObjectOptimisticLockingFailureException | AssertionFailure ex) {
                throw conflictoConcurrencia();
            } catch (DataIntegrityViolationException ex) {
                throw conflictoSlotOcupado();
            }
        }

        @Override
        @Transactional(readOnly = true)
        public List<HistorialCambiosCitaResponse> obtenerHistorialCambios(Long citaId) {
            if (!citaRepository.existsById(citaId)) {
                throw new ResourceNotFoundException("Cita", citaId);
            }
            return historialRepository.findByCitaIdOrderByCreatedAtDesc(citaId)
                    .stream()
                    .map(h -> HistorialCambiosCitaResponse.builder()
                            .id(h.getId())
                            .fechaAnterior(h.getFechaAnterior())
                            .horaAnterior(h.getHoraAnterior())
                            .medicoAnteriorId(h.getMedicoAnteriorId())
                            .fechaNueva(h.getFechaNueva())
                            .horaNueva(h.getHoraNueva())
                            .medicoNuevoId(h.getMedicoNuevoId())
                            .motivo(h.getMotivo())
                            .modificadoPor(h.getModificadoPor())
                            .creadoEn(h.getCreatedAt())
                            .build())
                    .toList();
        }

        // ─────────────────────────────────────────────────────────────
        // Detalle y actualización de cita (panel admin/médico)
        // ─────────────────────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public CitaDetalleResponse obtenerDetalleCita(Long citaId) {
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", citaId));

            PacienteResumenDTO paciente = pacientesApi.obtenerResumenPorId(cita.getPacienteId());
            MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(cita.getMedicoId());

            boolean esPrimera = !citaRepository.existsByPacienteIdAndIdLessThan(cita.getPacienteId(), citaId);

            return CitaDetalleResponse.builder()
                    .id(cita.getId())
                    .pacienteNombre(paciente.getNombres() + " " + paciente.getApellidos())
                    .pacienteDocumento(paciente.getDocumento())
                    .pacienteCelular(paciente.getCelular())
                    .pacienteCorreo(paciente.getCorreo())
                    .medicoNombre(medico.getNombresCompletos())
                    .especialidad(medico.getEspecialidad())
                    .fecha(cita.getFecha())
                    .hora(cita.getHora())
                    .estado(cita.getEstado())
                    .observaciones(cita.getObservaciones())
                    .esPrimeraCita(esPrimera)
                    .build();
        }

        @Override
        public CitaResponse actualizarCita(Long citaId, ActualizarCitaRequest request) {
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", citaId));

            boolean hayCambiosCita = request.getNuevoEstado() != null || request.getNuevasObservaciones() != null;
            boolean hayCambiosPaciente = request.getPacienteNombres() != null
                    || request.getPacienteApellidos() != null
                    || request.getPacienteDocumento() != null
                    || request.getPacienteCelular() != null
                    || request.getPacienteCorreo() != null;

            if (!hayCambiosCita && !hayCambiosPaciente) {
                throw new BusinessRuleException("Debe especificar al menos un campo a actualizar");
            }

            // Regla: MEDICO solo puede modificar la primera cita de un paciente
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esMedico = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"));

            if (esMedico && citaRepository.existsByPacienteIdAndIdLessThan(cita.getPacienteId(), citaId)) {
                throw new BusinessRuleException("Solo puedes modificar la primera cita de este paciente");
            }

            // Validar transicion de estado
            if (request.getNuevoEstado() != null) {
                String actual = cita.getEstado();
                String nuevo = request.getNuevoEstado();
                boolean transicionValida = "PROGRAMADA".equals(actual) && ("ATENDIDA".equals(nuevo) || "CANCELADA".equals(nuevo));
                if (!transicionValida) {
                    throw new BusinessRuleException("Transicion de estado no permitida: " + actual + " -> " + nuevo);
                }
                cita.setEstado(nuevo);
            }

            if (request.getNuevasObservaciones() != null) {
                cita.setObservaciones(request.getNuevasObservaciones());
            }

            citaRepository.save(cita);

            if (hayCambiosPaciente) {
                ActualizarPacienteDTO datosP = new ActualizarPacienteDTO(
                        request.getPacienteNombres(),
                        request.getPacienteApellidos(),
                        request.getPacienteDocumento(),
                        request.getPacienteCelular(),
                        request.getPacienteCorreo()
                );
                pacientesApi.actualizarDatosPaciente(cita.getPacienteId(), datosP);
            }

            UUID usuarioId = obtenerUsuarioIdAutenticado();
            auditService.registrar(
                    usuarioId,
                    "ACTUALIZAR",
                    "CITA",
                    cita.getId(),
                    "{\"nuevoEstado\":\"" + request.getNuevoEstado() + "\"}",
                    "N/A"
            );

            publicarCambioAgenda(cita.getMedicoId(), cita.getFecha(), citaId, "CITA_ACTUALIZADA");

            PacienteResumenDTO paciente = pacientesApi.obtenerResumenPorId(cita.getPacienteId());
            MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(cita.getMedicoId());
            return mapToResponse(cita, paciente, medico);
        }

        private void publicarCambioAgenda(Long medicoId, LocalDate fecha, Long citaId, String accion) {
            eventPublisher.publishEvent(new AgendaDinamicaChangedEvent(medicoId, fecha, citaId, accion));
        }

        private void adquirirBloqueoOptimistaAgenda(Long medicoId, LocalDate fecha) {
            AgendaDiaLock lock = agendaDiaLockRepository.findByMedicoIdAndFecha(medicoId, fecha)
                    .orElseGet(() -> crearLockAgendaDia(medicoId, fecha));

            lock.touch();
            agendaDiaLockRepository.saveAndFlush(lock);
        }

        private AgendaDiaLock crearLockAgendaDia(Long medicoId, LocalDate fecha) {
            AgendaDiaLock lock = new AgendaDiaLock();
            lock.setMedicoId(medicoId);
            lock.setFecha(fecha);
            lock.touch();

            try {
                return agendaDiaLockRepository.saveAndFlush(lock);
            } catch (DataIntegrityViolationException ex) {
                // If another transaction created the same (medico, fecha) row first,
                // avoid any further query in this persistence context after failed flush.
                throw new AgendaLockConcurrencyException(ex);
            }
        }

        private BusinessRuleException conflictoConcurrencia() {
            return new BusinessRuleException("La agenda fue modificada concurrentemente. Intente nuevamente");
        }

        private BusinessRuleException conflictoSlotOcupado() {
            return new BusinessRuleException("El horario seleccionado ya esta ocupado");
        }

        private static final class AgendaLockConcurrencyException extends RuntimeException {
            private AgendaLockConcurrencyException(Throwable cause) {
                super(cause);
            }
        }

        private LocalTime parseHora(String hora) {
            try {
                return LocalTime.parse(hora);
            } catch (DateTimeParseException ex) {
                throw new BusinessRuleException("La hora debe tener formato HH:mm:ss");
            }
        }

        private UUID obtenerUsuarioIdAutenticado() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return null;
            }

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String keycloakUserId = jwtAuth.getToken().getSubject();
                // Prefer internal DB UUID; fall back to Keycloak subject UUID directly
                // (covers admin/bootstrap users not registered through the app)
                return authApi.findByKeycloakId(keycloakUserId)
                        .map(com.piedrazul.backend.auth.api.dto.UsuarioInfoDto::getId)
                        .orElseGet(() -> {
                            try { return UUID.fromString(keycloakUserId); }
                            catch (IllegalArgumentException e) { return null; }
                        });
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof String principalStr) {
                return authApi.findByKeycloakId(principalStr)
                        .map(com.piedrazul.backend.auth.api.dto.UsuarioInfoDto::getId)
                        .orElseGet(() -> {
                            try { return UUID.fromString(principalStr); }
                            catch (IllegalArgumentException e) { return null; }
                        });
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
            LocalDate manana = LocalDate.now().plusDays(1);
            if (desde == null || desde.isBefore(manana)) {
                return manana;
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
        ) {
        }

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

        private int calcularTotalSlots(HorarioAtencionDTO horario, LocalDate fecha) {
            if (horario.getDiasAtencion() == null || !horario.getDiasAtencion().contains(fecha.getDayOfWeek())) {
                return 0;
            }
            if (horario.getHoraInicio() == null || horario.getHoraFin() == null || horario.getIntervaloMinutos() <= 0) {
                return 0;
            }
            if (!horario.getHoraInicio().isBefore(horario.getHoraFin())) {
                return 0;
            }

            long minutosJornada = java.time.Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toMinutes();
            return (int) (minutosJornada / horario.getIntervaloMinutos());
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
                    LocalTime inicioLibre = calcularInicioLibreAccionable(cursor, cita.getHora(), horario);
                    if (inicioLibre != null) {
                        slots.add(new SlotPanel(inicioLibre, null));
                    }
                }

                slots.add(new SlotPanel(cita.getHora(), cita));

                LocalTime fin = finCita(cita, medicoId);
                if (fin.isAfter(cursor)) {
                    cursor = fin;
                }
            }

            if (cursor.isBefore(horario.getHoraFin())) {
                LocalTime inicioLibre = calcularInicioLibreAccionable(cursor, horario.getHoraFin(), horario);
                if (inicioLibre != null) {
                    slots.add(new SlotPanel(inicioLibre, null));
                }
            }

            return slots;
        }

        private LocalTime calcularInicioLibreAccionable(LocalTime inicioHueco, LocalTime limiteHueco, HorarioAtencionDTO horario) {
            if (!inicioHueco.isBefore(limiteHueco)) {
                return null;
            }

            LocalTime inicioAlineado = alinearAlIntervalo(inicioHueco, horario);
            if (inicioAlineado == null || !inicioAlineado.isBefore(limiteHueco)) {
                return null;
            }

            int duracionEstandar = Math.max(1, horario.getIntervaloMinutos());
            LocalTime finRequerido = inicioAlineado.plusMinutes(duracionEstandar);
            if (finRequerido.isAfter(limiteHueco) || finRequerido.isAfter(horario.getHoraFin())) {
                return null;
            }

            return inicioAlineado;
        }

        private LocalTime alinearAlIntervalo(LocalTime hora, HorarioAtencionDTO horario) {
            if (hora == null || horario.getHoraInicio() == null || horario.getIntervaloMinutos() <= 0) {
                return null;
            }

            if (hora.isBefore(horario.getHoraInicio())) {
                return horario.getHoraInicio();
            }

            long minutosDesdeInicio = java.time.Duration.between(horario.getHoraInicio(), hora).toMinutes();
            long residuo = minutosDesdeInicio % horario.getIntervaloMinutos();
            if (residuo == 0) {
                return hora;
            }

            return hora.plusMinutes(horario.getIntervaloMinutos() - residuo);
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

        /**
         * {@inheritDoc}
         *
         * IMPLEMENTAR:
         *  1. Calcular porcentajeOcupacion:
         *       totalSlots = suma de médicos activos × días del rango × slotsPerDía
         *       porcentaje = (totalCitas / totalSlots) * 100
         *     (simplificación aceptable para Sprint 1: usar totalCitas / totalAtendidas)
         */
        public ResumenCitasDto obtenerResumenCitas(LocalDate desde, LocalDate hasta) {

            List<Object[]> conteos = citaRepository.countByEstadoBetweenFechas(desde, hasta);

            long programadas = 0;
            long atendidas   = 0;
            long canceladas  = 0;

            for (Object[] fila : conteos) {
                String estado = (String) fila[0];
                long   count  = (Long)   fila[1];
                switch (estado) {
                    case "PROGRAMADA" -> programadas = count;
                    case "ATENDIDA"   -> atendidas   = count;
                    case "CANCELADA"  -> canceladas  = count;
                }
            }

            long total = programadas + atendidas + canceladas;

            double porcentaje = 0.0;

            return ResumenCitasDto.builder()
                    .desde(desde)
                    .hasta(hasta)
                    .totalCitas(total)
                    .citasProgramadas(programadas)
                    .citasAtendidas(atendidas)
                    .citasCanceladas(canceladas)
                    .porcentajeOcupacion(porcentaje)
                    .build();
        }

        /**
         * {@inheritDoc}
         *
         * IMPLEMENTAR: ya usa el método correcto del repositorio.
         * Verificar que el umbral "CANCELADA" sea consistente con los
         * estados usados en Cita.estado (ver EstadoCita si se crea un enum).
         */
        @Override
        public boolean tieneCitasFuturas(Long pacienteId) {
            return citaRepository
                    .countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(
                            pacienteId, "CANCELADA", LocalDate.now()) > 0;
        }
}