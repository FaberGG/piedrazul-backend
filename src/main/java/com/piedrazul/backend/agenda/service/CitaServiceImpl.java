package com.piedrazul.backend.agenda.service;

import com.piedrazul.backend.agenda.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.dto.AgendaResponse;
import com.piedrazul.backend.agenda.dto.CitaResponse;
import com.piedrazul.backend.agenda.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.repository.CitaRepository;
import com.piedrazul.backend.agenda.repository.MedicoRepository;
import com.piedrazul.backend.agenda.repository.PacienteRepository;
import com.piedrazul.backend.shared.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementación del servicio de citas (módulo AGENDA).
 *
 * DEPENDENCIAS NECESARIAS para implementar cada método:
 *  - citaRepository       → persistir y consultar citas
 *  - pacienteRepository   → buscar/crear paciente por documento
 *  - medicoRepository     → validar médico activo
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
    private final PacienteRepository    pacienteRepository;
    private final MedicoRepository      medicoRepository;
    private final DisponibilidadService disponibilidadService;
    private final AuditService          auditService;

    public CitaServiceImpl(CitaRepository citaRepository,
                           PacienteRepository pacienteRepository,
                           MedicoRepository medicoRepository,
                           DisponibilidadService disponibilidadService,
                           AuditService auditService) {
        this.citaRepository        = citaRepository;
        this.pacienteRepository    = pacienteRepository;
        this.medicoRepository      = medicoRepository;
        this.disponibilidadService = disponibilidadService;
        this.auditService          = auditService;
    }

    // ─────────────────────────────────────────────────────────────
    // RF1 — Listar agenda de un médico por fecha
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * PASOS A IMPLEMENTAR:
     *  1. medicoRepository.findById(medicoId) → lanzar ResourceNotFoundException si no existe
     *  2. citaRepository.findByMedicoIdAndFecha(medicoId, fecha) → List<Cita>
     *  3. disponibilidadService.calcularHorariosDisponibles(medicoId, fecha) → List<LocalTime>
     *  4. Mapear cada Cita → CitaResponse
     *  5. Calcular porcentajeOcupacion = (citasActivas / totalSlots) * 100
     *  6. Construir y retornar AgendaResponse
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
     *
     * PASOS A IMPLEMENTAR:
     *  1. medicoRepository.findById(request.medicoId) → ResourceNotFoundException si inactivo
     *  2. pacienteRepository.findByDocumento(request.documento)
     *       → si no existe: crear nuevo Paciente y guardar
     *  3. disponibilidadService.estaDisponible(medicoId, fecha, hora)
     *       → si false: throw BusinessRuleException("Horario no disponible")
     *  4. Cita.builder()...estado("PROGRAMADA").creadoPor(usuarioAutenticadoId).build()
     *  5. citaRepository.save(cita)
     *  6. auditService.registrar(userId, "CREAR_CITA", "Cita", cita.getId(), detalles, ip)
     *  7. Mapear Cita → CitaResponse y retornar
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
     * PASOS A IMPLEMENTAR:
     *  1. Obtener pacienteId desde SecurityContextHolder (usuario autenticado)
     *  2. Verificar límite: citaRepository.countByPacienteId...AndFecha >= 3
     *       → throw BusinessRuleException("Límite de 3 citas futuras alcanzado")
     *  3. disponibilidadService.estaDisponible(request.medicoId, request.fecha, request.hora)
     *       → si false: throw BusinessRuleException("Horario no disponible")
     *  4. citaRepository.save(cita) — considerar @Lock(PESSIMISTIC_WRITE) para concurrencia
     *  5. auditService.registrar(...)
     *  6. Retornar CitaResponse
     */
    @Override
    public CitaResponse agendarAutonomo(AgendarAutonomoRequest request) {
        throw new UnsupportedOperationException("TODO RF3: implementar agendarAutonomo");
    }
}

