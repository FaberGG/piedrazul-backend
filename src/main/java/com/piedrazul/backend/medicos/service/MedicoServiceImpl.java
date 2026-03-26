package com.piedrazul.backend.medicos.service;

import com.piedrazul.backend.auth.api.AuthApi;
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
    private final AuthApi authApi;

    @Override
    public void crearMedico(Long usuarioId, String nombres, String apellidos,
                            String especialidad, String tipo) {

        if (usuarioId != null && !authApi.existeUsuarioActivo(usuarioId)) {
            throw new IllegalArgumentException(
                    "El usuario con id " + usuarioId + " no existe o no está activo"
            );
        }

        Medico medico = Medico.builder()
                .usuarioId(usuarioId)
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