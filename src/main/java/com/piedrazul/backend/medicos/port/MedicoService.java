package com.piedrazul.backend.medicos.port;

import com.piedrazul.backend.auth.domain.Usuario;
import org.springframework.modulith.NamedInterface;

@NamedInterface
public interface MedicoService {

    void crearMedico(Usuario usuario, String nombres, String apellidos,
                     String especialidad, String tipo);


}
