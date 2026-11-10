package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.util.List;

/**
 * Eslabon de la cadena de validacion (Chain of Responsibility, RF1).
 *
 * <p>Cada eslabon ejecuta una sola verificacion. Si la solicitud no la pasa,
 * corta la cadena y devuelve el rechazo; si la pasa, delega en el siguiente
 * eslabon. Ningun eslabon sabe cuantos hay despues de el, por lo que agregar
 * una verificacion nueva no obliga a modificar las existentes.</p>
 */
public abstract class ValidadorSolicitud {

    private ValidadorSolicitud siguiente;

    /**
     * Engancha el eslabon que corre despues de este.
     *
     * @param siguiente eslabon siguiente
     * @return el mismo eslabon recibido, para encadenar llamadas:
     *         {@code a.siguiente(b).siguiente(c)}
     */
    public ValidadorSolicitud siguiente(ValidadorSolicitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    /**
     * Valida la solicitud en este eslabon y, si la aprueba, en los siguientes.
     *
     * @param solicitud             solicitud a validar
     * @param pendientesDelEmpleado solicitudes pendientes del mismo empleado,
     *                              ya cargadas por el servicio de aplicacion
     * @return el primer rechazo encontrado, o un resultado valido si toda la
     *         cadena aprueba
     */
    public ResultadoValidacion validar(SolicitudReembolso solicitud,
                                       List<SolicitudReembolso> pendientesDelEmpleado) {
        ResultadoValidacion propio = validarPropio(solicitud, pendientesDelEmpleado);
        if (!propio.valida() || siguiente == null) {
            return propio;
        }
        return siguiente.validar(solicitud, pendientesDelEmpleado);
    }

    /**
     * Verificacion propia de este eslabon, sin ocuparse del resto de la cadena.
     *
     * @param solicitud             solicitud a validar
     * @param pendientesDelEmpleado solicitudes pendientes del mismo empleado
     * @return resultado de esta unica verificacion
     */
    protected abstract ResultadoValidacion validarPropio(
            SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado);
}
