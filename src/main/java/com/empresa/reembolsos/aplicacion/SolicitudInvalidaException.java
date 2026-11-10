package com.empresa.reembolsos.aplicacion;

/**
 * La solicitud no paso la cadena de validacion. El mensaje es el motivo de
 * rechazo que devolvio el validador que la detuvo.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String motivoRechazo) {
        super(motivoRechazo);
    }
}
