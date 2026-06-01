package com.piedrazul.backend.agenda;

import com.piedrazul.backend.agenda.internal.domain.AgendaDiaLock;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.repository.AgendaDiaLockRepository;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.agenda.internal.service.CitaServiceImpl;
import com.piedrazul.backend.agenda.internal.service.DisponibilidadService;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.shared.audit.service.AuditService;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para RF1 — Listar agenda de un médico por fecha.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock private CitaRepository citaRepository;
    @Mock private AgendaDiaLockRepository agendaDiaLockRepository;
    @Mock private DisponibilidadService disponibilidadService;
    @Mock private AuditService auditService;
    @Mock private MedicosApi medicosApi;
    @Mock private PacientesApi pacientesApi;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CitaServiceImpl citaService;

    private MedicoResumenDTO medicoActivo;
    private HorarioAtencionDTO horarioEstandar;
    private LocalDate fechaLunes;

    @BeforeEach
    void setUp() {
        medicoActivo = MedicoResumenDTO.builder()
                .id(1L)
                .nombresCompletos("Clara Ines Cordoba")
                .especialidad("TERAPIA_NEURAL")
                .activo(true)
                .build();

        fechaLunes = LocalDate.of(2026, 3, 23); // Lunes

        horarioEstandar = HorarioAtencionDTO.builder()
                .horaInicio(LocalTime.of(7, 0))
                .horaFin(LocalTime.of(12, 0))
                .intervaloMinutos(15)
                .diasAtencion(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("RF1 — Debe retornar agenda con citas y slots disponibles correctamente")
    void listarAgendaMedico_debeRetornarAgendaCompleta() {
        // ARRANGE
        Cita cita1 = Cita.builder()
                .id(101L)
                .pacienteId(10L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(7, 0))
                .estado("PROGRAMADA")
                .build();

        PacienteResumenDTO paciente = PacienteResumenDTO.builder()
                .id(10L)
                .documento("122321")
                .nombres("Ana")
                .apellidos("Perez")
                .build();

        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoActivo);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of(cita1));
        when(pacientesApi.obtenerResumenPorId(10L)).thenReturn(paciente);
        when(disponibilidadService.calcularHorariosDisponibles(1L, fechaLunes))
                .thenReturn(List.of(LocalTime.of(7, 15), LocalTime.of(7, 30)));

        // ACT
        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, fechaLunes);

        // ASSERT
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getMedicoId()).isEqualTo(1L);
        assertThat(respuesta.getMedicoNombre()).isEqualTo("Clara Ines Cordoba");
        assertThat(respuesta.getEspecialidad()).isEqualTo("TERAPIA_NEURAL");
        assertThat(respuesta.getFecha()).isEqualTo(fechaLunes);
        assertThat(respuesta.getCitas()).hasSize(1);
        assertThat(respuesta.getCitas().get(0).getPacienteNombre()).isEqualTo("Ana Perez");
        assertThat(respuesta.getCitas().get(0).getPacienteDocumento()).isEqualTo("122321");
        assertThat(respuesta.getHorariosDisponibles()).hasSize(2);
        assertThat(respuesta.getHorariosDisponibles()).containsExactly("07:15:00", "07:30:00");
        assertThat(respuesta.getTotalSlots()).isEqualTo(20); // 300 min / 15 = 20
        assertThat(respuesta.getSlotsOcupados()).isEqualTo(1);
        assertThat(respuesta.getPorcentajeOcupacion()).isEqualTo(5.0); // 1/20 * 100
    }

    @Test
    @DisplayName("RF1 — Debe lanzar excepción si el médico no existe")
    void listarAgendaMedico_debeLanzarExcepcionSiMedicoNoExiste() {
        // ARRANGE
        when(medicosApi.obtenerResumenMedico(99L)).thenReturn(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> citaService.listarAgendaMedico(99L, fechaLunes))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RF1 — Debe lanzar excepción si el médico está inactivo")
    void listarAgendaMedico_debeLanzarExcepcionSiMedicoInactivo() {
        MedicoResumenDTO medicoInactivo = MedicoResumenDTO.builder()
                .id(1L)
                .nombresCompletos("Medico Inactivo")
                .especialidad("GENERAL")
                .activo(false)
                .build();

        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoInactivo);

        assertThatThrownBy(() -> citaService.listarAgendaMedico(1L, fechaLunes))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("activo");
    }

    @Test
    @DisplayName("RF1 — Debe retornar 0% ocupación si no hay citas en el día")
    void listarAgendaMedico_sinCitasDebeMostrarCeroOcupacion() {
        // ARRANGE
        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoActivo);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of());
        when(disponibilidadService.calcularHorariosDisponibles(1L, fechaLunes))
                .thenReturn(List.of(LocalTime.of(7, 0), LocalTime.of(7, 15)));

        // ACT
        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, fechaLunes);

        // ASSERT
        assertThat(respuesta.getCitas()).isEmpty();
        assertThat(respuesta.getSlotsOcupados()).isZero();
        assertThat(respuesta.getPorcentajeOcupacion()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RF1 — Las citas CANCELADAS no deben contar como ocupadas")
    void listarAgendaMedico_citasCanceladasNoContanComoOcupadas() {
        // ARRANGE
        Cita citaCancelada = Cita.builder()
                .id(200L)
                .pacienteId(20L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(7, 0))
                .estado("CANCELADA")
                .build();

        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoActivo);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of(citaCancelada));
        when(disponibilidadService.calcularHorariosDisponibles(1L, fechaLunes))
                .thenReturn(List.of(LocalTime.of(7, 0)));

        // ACT
        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, fechaLunes);

        // ASSERT
        assertThat(respuesta.getSlotsOcupados()).isZero(); // La cancelada no cuenta
        assertThat(respuesta.getCitas()).isEmpty();
    }

    @Test
    @DisplayName("RF1 — Si el medico no atiende ese dia, no debe reportar slots teoricos")
    void listarAgendaMedico_medicoNoAtiendeDiaDebeRetornarTotalSlotsCero() {
        LocalDate domingo = LocalDate.of(2026, 3, 22);

        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoActivo);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(citaRepository.findByMedicoIdAndFecha(1L, domingo)).thenReturn(List.of());
        when(disponibilidadService.calcularHorariosDisponibles(1L, domingo)).thenReturn(List.of());

        AgendaResponse respuesta = citaService.listarAgendaMedico(1L, domingo);

        assertThat(respuesta.getTotalSlots()).isZero();
        assertThat(respuesta.getHorariosDisponibles()).isEmpty();
    }

    @Test
    @DisplayName("Agenda dinamica no debe mostrar 9:30 AM libre si no cabe una cita estandar de 30 minutos")
    void obtenerAgendaDinamica_noDebeMostrarHuecoInutilizableTrasPrioridad() {
        HorarioAtencionDTO horario30 = HorarioAtencionDTO.builder()
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0))
                .intervaloMinutos(30)
                .diasAtencion(List.of(DayOfWeek.MONDAY))
                .activo(true)
                .build();

        Cita base = Cita.builder()
                .id(1L)
                .pacienteId(10L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(9, 0))
                .duracionMinutos(15)
                .tipoCita("ESTANDAR")
                .estado("PROGRAMADA")
                .build();

        Cita prioridad = Cita.builder()
                .id(2L)
                .pacienteId(11L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(9, 15))
                .duracionMinutos(5)
                .tipoCita("PRIORIDAD")
                .estado("PROGRAMADA")
                .build();

        Cita siguiente = Cita.builder()
                .id(3L)
                .pacienteId(12L)
                .medicoId(1L)
                .fecha(fechaLunes)
                .hora(LocalTime.of(9, 45))
                .duracionMinutos(30)
                .tipoCita("ESTANDAR")
                .estado("PROGRAMADA")
                .build();

        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoActivo);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horario30);
        when(citaRepository.findByMedicoIdAndFecha(1L, fechaLunes)).thenReturn(List.of(base, prioridad, siguiente));

        AgendaDinamicaResponse respuesta = citaService.obtenerAgendaDinamica(1L, fechaLunes);

        List<String> horasLibres = respuesta.getBloques().stream()
                .flatMap(bloque -> bloque.getSlots().stream())
                .filter(slot -> "LIBRE".equals(slot.getEstado()))
                .map(slot -> slot.getHora())
                .collect(Collectors.toList());

        assertThat(horasLibres).doesNotContain("9:30 AM");
        assertThat(horasLibres).contains("10:30 AM");
    }

    @Test
    @DisplayName("RF2 — Si existe conflicto de version en lock de agenda debe informar concurrencia")
    void crearCitaManual_conflictoOptimistaDebeInformarConcurrencia() {
        LocalDate fechaFutura = LocalDate.now().plusDays(2);
        AgendaDiaLock lock = new AgendaDiaLock();
        lock.setMedicoId(1L);
        lock.setFecha(fechaFutura);

        when(agendaDiaLockRepository.findByMedicoIdAndFecha(1L, fechaFutura))
                .thenReturn(Optional.of(lock));
        when(agendaDiaLockRepository.saveAndFlush(any(AgendaDiaLock.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(AgendaDiaLock.class, 1L));

        CrearCitaManualRequest request = new CrearCitaManualRequest(
                "1234567890",
                "Ana",
                "Perez",
                "3001234567",
                "FEMENINO",
                LocalDate.of(1990, 1, 1),
                "ana@mail.com",
                1L,
                "08:00:00",
                fechaFutura,
                "Control"
        );

        assertThatThrownBy(() -> citaService.crearCitaManual(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("concurrentemente");
    }

    @Test
    @DisplayName("RF2 — Si el slot colisiona por concurrencia DB debe informar ocupado")
    void crearCitaManual_colisionUnicaDebeInformarOcupado() {
        LocalDate fecha = LocalDate.now().plusDays(2);
        AgendaDiaLock lock = new AgendaDiaLock();
        lock.setMedicoId(1L);
        lock.setFecha(fecha);

        PacienteResumenDTO paciente = PacienteResumenDTO.builder()
                .id(10L)
                .documento("1234567890")
                .nombres("Ana")
                .apellidos("Perez")
                .build();

        when(agendaDiaLockRepository.findByMedicoIdAndFecha(1L, fecha)).thenReturn(Optional.of(lock));
        when(agendaDiaLockRepository.saveAndFlush(any(AgendaDiaLock.class))).thenReturn(lock);
        when(medicosApi.obtenerResumenMedico(1L)).thenReturn(medicoActivo);
        when(medicosApi.obtenerHorarioAtencion(1L)).thenReturn(horarioEstandar);
        when(disponibilidadService.estaDisponible(1L, fecha, LocalTime.of(8, 0))).thenReturn(true);
        when(pacientesApi.obtenerOCrearPorDocumento(any())).thenReturn(paciente);
        when(citaRepository.save(any(Cita.class))).thenThrow(new DataIntegrityViolationException("unique"));

        CrearCitaManualRequest request = new CrearCitaManualRequest(
                "1234567890",
                "Ana",
                "Perez",
                "3001234567",
                "FEMENINO",
                LocalDate.of(1990, 1, 1),
                "ana@mail.com",
                1L,
                "08:00:00",
                fecha,
                "Control"
        );

        assertThatThrownBy(() -> citaService.crearCitaManual(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ocupado");
    }
}
