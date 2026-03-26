package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.ConfiguracionGlobal;
import com.piedrazul.backend.agenda.internal.domain.DiaNoLaboral;
import com.piedrazul.backend.agenda.internal.dto.ConfiguracionAgendaRequest;
import com.piedrazul.backend.agenda.internal.dto.ConfiguracionAgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.DiaNoLaboralRequest;
import com.piedrazul.backend.agenda.internal.dto.DiaNoLaboralResponse;
import com.piedrazul.backend.agenda.internal.repository.ConfiguracionGlobalRepository;
import com.piedrazul.backend.agenda.internal.repository.DiaNoLaboralRepository;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracionAgendaServiceImpl implements ConfiguracionAgendaService {

    private static final String CLAVE_VENTANA = "VENTANA_AGENDAMIENTO_SEMANAS";
    private static final int VENTANA_DEFAULT = 4;

    private final ConfiguracionGlobalRepository configuracionRepository;
    private final DiaNoLaboralRepository diaNoLaboralRepository;

    @Override
    public ConfiguracionAgendaResponse obtenerConfiguracion() {
        int ventana = configuracionRepository.findByClave(CLAVE_VENTANA)
                .map(c -> Integer.parseInt(c.getValor()))
                .orElse(VENTANA_DEFAULT);
        return new ConfiguracionAgendaResponse(ventana);
    }

    @Override
    @Transactional
    public ConfiguracionAgendaResponse actualizarVentana(ConfiguracionAgendaRequest request) {
        ConfiguracionGlobal config = configuracionRepository.findByClave(CLAVE_VENTANA)
                .orElseGet(() -> ConfiguracionGlobal.builder()
                        .clave(CLAVE_VENTANA)
                        .descripcion("Semanas hacia adelante habilitadas para agendar citas")
                        .build());

        config.setValor(String.valueOf(request.getVentanaAgendamientoSemanas()));
        configuracionRepository.save(config);
        return new ConfiguracionAgendaResponse(request.getVentanaAgendamientoSemanas());
    }

    @Override
    @Transactional
    public DiaNoLaboralResponse agregarDiaNoLaboral(DiaNoLaboralRequest request) {
        if (diaNoLaboralRepository.existsByFecha(request.getFecha())) {
            throw new IllegalArgumentException(
                    "Ya existe un día no laboral registrado para: " + request.getFecha()
            );
        }

        DiaNoLaboral dia = DiaNoLaboral.builder()
                .fecha(request.getFecha())
                .descripcion(request.getDescripcion())
                .build();

        return toResponse(diaNoLaboralRepository.save(dia));
    }

    @Override
    @Transactional
    public void eliminarDiaNoLaboral(Long id) {
        if (!diaNoLaboralRepository.existsById(id)) {
            throw new ResourceNotFoundException("DiaNoLaboral", id);
        }
        diaNoLaboralRepository.deleteById(id);
    }

    @Override
    public List<DiaNoLaboralResponse> listarDiasNoLaborales() {
        return diaNoLaboralRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DiaNoLaboralResponse toResponse(DiaNoLaboral d) {
        return DiaNoLaboralResponse.builder()
                .id(d.getId())
                .fecha(d.getFecha())
                .descripcion(d.getDescripcion())
                .build();
    }
}