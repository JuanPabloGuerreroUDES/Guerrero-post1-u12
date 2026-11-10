package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Interesado en los cambios de estado de una solicitud (Observer, RF3).
 *
 * <p>El servicio de aplicacion avisa a todos los observadores registrados
 * cada vez que una solicitud cambia de estado. Cada observador decide por su
 * cuenta si reacciona y como; el servicio no conoce esos detalles. Agregar un
 * interesado nuevo consiste en crear otra implementacion anotada con
 * {@code @Component}.</p>
 */
public interface ObservadorCambioEstado {

    /**
     * Se invoca despues de guardar el cambio de estado.
     *
     * @param solicitud      solicitud con su nuevo estado ya asignado
     * @param estadoAnterior estado que tenia antes del cambio
     */
    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}
