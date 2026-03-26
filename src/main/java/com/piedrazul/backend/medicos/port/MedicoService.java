package com.piedrazul.backend.medicos.port;

import com.piedrazul.backend.auth.internal.domain.Usuario;
import org.springframework.modulith.NamedInterface;

@NamedInterface
public interface MedicoService {

    void crearMedico(Long usuarioID, String nombres, String apellidos,
                     String especialidad, String tipo);


}
