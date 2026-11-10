package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fabrica de solicitudes validas para las pruebas. Cada prueba parte de una
 * solicitud correcta y cambia solo el campo que quiere ejercitar.
 */
public final class SolicitudesDePrueba {

    private SolicitudesDePrueba() {
    }

    public static SolicitudReembolso solicitud(CategoriaGasto categoria, String monto) {
        var solicitud = new SolicitudReembolso();
        solicitud.setEmpleadoId("EMP-001");
        solicitud.setCategoria(categoria);
        solicitud.setMontoSolicitado(new BigDecimal(monto));
        solicitud.setFechaGasto(LocalDate.of(2026, 11, 5));
        solicitud.setDiasViaje(1);
        solicitud.setDocumentosSoporte(true);
        return solicitud;
    }
}
