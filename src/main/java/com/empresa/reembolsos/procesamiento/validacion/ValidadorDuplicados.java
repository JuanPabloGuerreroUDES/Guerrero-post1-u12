package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * Verifica que el empleado no tenga ya otra solicitud pendiente de la misma
 * categoria en el mismo periodo (mes y anio del gasto).
 *
 * <p>No consulta la base de datos: compara contra la lista de pendientes que
 * el servicio de aplicacion ya cargo, de modo que este paquete no depende de
 * infraestructura.</p>
 */
@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    // VIOLACION INTENCIONAL de la Regla 1: procesamiento no debe conocer infraestructura
    private RepositorioSolicitudesJpa repositorio;

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud,
                                                List<SolicitudReembolso> pendientes) {
        var periodo = YearMonth.from(solicitud.getFechaGasto());
        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && YearMonth.from(p.getFechaGasto()).equals(periodo));
        return existeDuplicado
                ? ResultadoValidacion.rechazar(
                        "Ya existe una solicitud pendiente de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}
