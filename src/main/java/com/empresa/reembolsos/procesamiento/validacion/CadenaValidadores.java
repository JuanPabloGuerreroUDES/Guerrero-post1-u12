package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Arma la cadena de validadores y expone un unico punto de entrada.
 *
 * <p>Orden: documentos, monto, duplicados. Los dos primeros solo leen campos
 * de la solicitud (costo constante); el de duplicados recorre la lista de
 * pendientes del empleado, asi que va de ultimo para ejecutarse solo cuando
 * las verificaciones baratas ya pasaron (ver Decision de diseno 1 del README).</p>
 */
@Component
public class CadenaValidadores {

    private final ValidadorSolicitud primero;

    public CadenaValidadores(ValidadorDocumentosSoporte documentos,
                             ValidadorMontoMaximo monto,
                             ValidadorDuplicados duplicados) {
        documentos.siguiente(monto).siguiente(duplicados);
        this.primero = documentos;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud,
                                       List<SolicitudReembolso> pendientes) {
        return primero.validar(solicitud, pendientes);
    }
}
