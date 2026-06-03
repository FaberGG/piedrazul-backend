package com.piedrazul.backend.medicos.internal.service;

import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.medicos.api.dto.RegistroMedicoDTO;
import com.piedrazul.backend.medicos.internal.domain.Medico;
import com.piedrazul.backend.medicos.internal.dto.ConfiguracionAgendaMedicoResponse;
import com.piedrazul.backend.medicos.internal.dto.ConfigurarAgendaMedicoRequest;
import com.piedrazul.backend.medicos.internal.dto.MedicoListadoResponse;
import com.piedrazul.backend.medicos.internal.repository.MedicosRepository;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Implementacion minima del facade de medicos para habilitar inyeccion de MedicosApi.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MedicosFacade implements MedicosApi {

    private static final LocalTime HORA_INICIO_DEFAULT = LocalTime.of(7, 0);
    private static final LocalTime HORA_FIN_DEFAULT = LocalTime.of(12, 0);
    private static final int INTERVALO_DEFAULT = 15;
    private static final List<DayOfWeek> DIAS_DEFAULT = List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
    );
    private static final Set<Integer> INTERVALOS_VALIDOS = Set.of(5, 10, 15, 20, 30, 45, 60);

    private final MedicosRepository medicosRepository;

    @Override
    @Transactional
    public void registrarMedicoConUsuario(RegistroMedicoDTO request) {
        if (request.getUsuarioId() == null) {
            throw new BusinessRuleException("El usuarioId es obligatorio para registrar un medico");
        }

        if (medicosRepository.existsByUsuarioId(request.getUsuarioId())) {
            throw new BusinessRuleException("El usuario ya tiene un medico vinculado");
        }

        medicosRepository.save(Medico.builder()
                .usuarioId(request.getUsuarioId())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .especialidad(request.getEspecialidad())
                .tipo(request.getTipo())
                .estado("ACTIVO")
                .horaInicioAtencion(HORA_INICIO_DEFAULT)
                .horaFinAtencion(HORA_FIN_DEFAULT)
                .intervaloMinutos(INTERVALO_DEFAULT)
                .diasAtencion(serializarDias(DIAS_DEFAULT))
                .build());
    }

    @Override
    public HorarioAtencionDTO obtenerHorarioAtencion(Long medicoId) {
        return medicosRepository.findById(medicoId)
                .map(this::toHorarioAtencion)
                .orElseThrow(() -> new ResourceNotFoundException("Medico", medicoId));
    }

    @Override
    public MedicoResumenDTO obtenerResumenMedico(Long medicoId) {
        return medicosRepository.findById(medicoId)
                .map(this::toResumen)
                .orElseThrow(() -> new ResourceNotFoundException("Medico", medicoId));
    }

    @Override
    public List<MedicoResumenDTO> listarMedicosActivos() {
        return medicosRepository.findByEstadoIgnoreCase("ACTIVO")
                .stream()
                .map(this::toResumen)
                .toList();
    }

    public MedicoListadoResponse obtenerMedicoActual() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UUID usuarioId = UUID.fromString(auth.getToken().getSubject());
        return medicosRepository.findByUsuarioId(usuarioId)
                .map(this::toListado)
                .orElseThrow(() -> new ResourceNotFoundException("Medico", usuarioId));
    }

    public List<MedicoListadoResponse> listarMedicosActivos(String especialidad) {
        List<Medico> medicos = (especialidad == null || especialidad.isBlank())
                ? medicosRepository.findByEstadoIgnoreCase("ACTIVO")
                : medicosRepository.findByEstadoIgnoreCaseAndEspecialidadIgnoreCase("ACTIVO", especialidad.trim());

        return medicos.stream()
                .map(this::toListado)
                .toList();
    }

    public ConfiguracionAgendaMedicoResponse obtenerConfiguracionAgenda(Long medicoId) {
        Medico medico = medicosRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Medico", medicoId));

        return toConfiguracionResponse(medico);
    }

    @Transactional
    public ConfiguracionAgendaMedicoResponse configurarAgenda(Long medicoId, ConfigurarAgendaMedicoRequest request) {
        validarConfiguracion(request);

        Medico medico = medicosRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Medico", medicoId));

        medico.setHoraInicioAtencion(request.getHoraInicio());
        medico.setHoraFinAtencion(request.getHoraFin());
        medico.setIntervaloMinutos(request.getIntervaloMinutos());
        medico.setDiasAtencion(serializarDias(request.getDiasAtencion()));

        Medico guardado = medicosRepository.save(medico);
        return toConfiguracionResponse(guardado);
    }

    private HorarioAtencionDTO toHorarioAtencion(Medico medico) {
        LocalTime horaInicio = medico.getHoraInicioAtencion() != null ? medico.getHoraInicioAtencion() : HORA_INICIO_DEFAULT;
        LocalTime horaFin = medico.getHoraFinAtencion() != null ? medico.getHoraFinAtencion() : HORA_FIN_DEFAULT;
        int intervalo = medico.getIntervaloMinutos() != null ? medico.getIntervaloMinutos() : INTERVALO_DEFAULT;

        return HorarioAtencionDTO.builder()
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .intervaloMinutos(intervalo)
                .diasAtencion(deserializarDias(medico.getDiasAtencion()))
                .activo("ACTIVO".equalsIgnoreCase(medico.getEstado()))
                .build();
    }

    private MedicoResumenDTO toResumen(Medico medico) {
        return MedicoResumenDTO.builder()
                .id(medico.getId())
                .nombresCompletos((medico.getNombres() + " " + medico.getApellidos()).trim())
                .especialidad(medico.getEspecialidad())
                .activo("ACTIVO".equalsIgnoreCase(medico.getEstado()))
                .build();
    }

    private MedicoListadoResponse toListado(Medico medico) {
        return MedicoListadoResponse.builder()
                .id(medico.getId())
                .nombresCompletos((medico.getNombres() + " " + medico.getApellidos()).trim())
                .especialidad(medico.getEspecialidad())
                .tipo(medico.getTipo())
                .activo("ACTIVO".equalsIgnoreCase(medico.getEstado()))
                .intervaloMinutos(medico.getIntervaloMinutos() != null ? medico.getIntervaloMinutos() : INTERVALO_DEFAULT)
                .build();
    }

    private ConfiguracionAgendaMedicoResponse toConfiguracionResponse(Medico medico) {
        HorarioAtencionDTO horario = toHorarioAtencion(medico);
        int minutosJornada = (int) Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toMinutes();

        return ConfiguracionAgendaMedicoResponse.builder()
                .medicoId(medico.getId())
                .medicoNombre((medico.getNombres() + " " + medico.getApellidos()).trim())
                .especialidad(medico.getEspecialidad())
                .activo("ACTIVO".equalsIgnoreCase(medico.getEstado()))
                .diasAtencion(horario.getDiasAtencion())
                .horaInicio(horario.getHoraInicio())
                .horaFin(horario.getHoraFin())
                .intervaloMinutos(horario.getIntervaloMinutos())
                .capacidadDiaria(minutosJornada / horario.getIntervaloMinutos())
                .build();
    }

    private void validarConfiguracion(ConfigurarAgendaMedicoRequest request) {
        if (request.getHoraFin().isBefore(request.getHoraInicio()) || request.getHoraFin().equals(request.getHoraInicio())) {
            throw new BusinessRuleException("La hora de fin debe ser posterior a la hora de inicio");
        }

        long minutos = Duration.between(request.getHoraInicio(), request.getHoraFin()).toMinutes();
        if (minutos < 120 || minutos > 480) {
            throw new BusinessRuleException("La jornada debe estar entre 2 y 8 horas");
        }

        if (!INTERVALOS_VALIDOS.contains(request.getIntervaloMinutos())) {
            throw new BusinessRuleException("El intervalo debe ser uno de: 5, 10, 15, 20, 30, 45, 60 minutos");
        }

        if (request.getDiasAtencion() == null || request.getDiasAtencion().isEmpty()) {
            throw new BusinessRuleException("Debe configurar al menos un dia de atencion");
        }
    }

    private String serializarDias(List<DayOfWeek> diasAtencion) {
        return diasAtencion.stream()
                .map(Enum::name)
                .toList()
                .toString()
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "");
    }

    private List<DayOfWeek> deserializarDias(String diasAtencion) {
        if (diasAtencion == null || diasAtencion.isBlank()) {
            return DIAS_DEFAULT;
        }

        try {
            return Arrays.stream(diasAtencion.split(","))
                    .map(String::trim)
                    .filter(valor -> !valor.isBlank())
                    .map(valor -> DayOfWeek.valueOf(valor.toUpperCase(Locale.ROOT)))
                    .toList();
        } catch (IllegalArgumentException ex) {
            return DIAS_DEFAULT;
        }
    }
}

