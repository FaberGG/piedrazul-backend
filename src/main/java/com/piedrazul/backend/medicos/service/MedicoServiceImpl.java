package com.piedrazul.backend.medicos.service;

import com.piedrazul.backend.auth.domain.Usuario;
import com.piedrazul.backend.medicos.domain.Medico;
import com.piedrazul.backend.medicos.port.MedicoService;
import com.piedrazul.backend.medicos.repository.MedicosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service("medicoCreacionService")
@RequiredArgsConstructor
public class MedicoServiceImpl implements MedicoService {

    private final MedicosRepository medicoRepository;

    @Override
    public void crearMedico(Usuario usuario, String nombres, String apellidos,
                            String especialidad, String tipo) {
        Medico medico = Medico.builder()
                .usuario(usuario)
                .nombres(nombres)
                .apellidos(apellidos)
                .especialidad(especialidad)
                .tipo(tipo)
                .estado("ACTIVO")
                .horaInicioAtencion(LocalTime.of(7, 0))
                .horaFinAtencion(LocalTime.of(12, 0))
                .intervaloMinutos(15)
                .diasAtencion("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        medicoRepository.save(medico);
    }
}