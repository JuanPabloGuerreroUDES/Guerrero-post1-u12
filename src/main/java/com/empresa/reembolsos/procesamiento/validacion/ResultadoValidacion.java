package com.empresa.reembolsos.procesamiento.validacion;

/**
 * Resultado de una verificacion: valida, o rechazada con su motivo.
 *
 * @param valida        {@code true} si la solicitud paso la verificacion
 * @param motivoRechazo texto para el empleado cuando no es valida; {@code null} si lo es
 */
public record ResultadoValidacion(boolean valida, String motivoRechazo) {

    public static ResultadoValidacion ok() {
        return new ResultadoValidacion(true, null);
    }

    public static ResultadoValidacion rechazar(String motivo) {
        return new ResultadoValidacion(false, motivo);
    }
}
