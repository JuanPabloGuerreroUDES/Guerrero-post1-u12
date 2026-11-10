package com.empresa.reembolsos.aplicacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import com.empresa.reembolsos.procesamiento.calculo.SelectorCalculadora;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.empresa.reembolsos.procesamiento.validacion.CadenaValidadores;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de aplicacion: unico punto donde se coordinan los tres patrones
 * (validacion, calculo y notificacion) y unico que conoce el repositorio.
 */
@Service
public class SolicitudReembolsoService {

    private final CadenaValidadores validadores;
    private final SelectorCalculadora calculadoras;
    private final List<ObservadorCambioEstado> observadores;
    private final RepositorioSolicitudesJpa repositorio;

    public SolicitudReembolsoService(CadenaValidadores validadores,
                                     SelectorCalculadora calculadoras,
                                     List<ObservadorCambioEstado> observadores,
                                     RepositorioSolicitudesJpa repositorio) {
        this.validadores = validadores;
        this.calculadoras = calculadoras;
        this.observadores = observadores;
        this.repositorio = repositorio;
    }

    /**
     * Registra una solicitud nueva: la valida en todas sus etapas, calcula el
     * monto aprobado segun su categoria y la guarda en estado PENDIENTE.
     *
     * @throws SolicitudInvalidaException si algun validador la rechaza
     */
    public SolicitudReembolso crear(SolicitudReembolso solicitud) {
        var pendientes = repositorio.findByEmpleadoIdAndEstado(
                solicitud.getEmpleadoId(), EstadoSolicitud.PENDIENTE);
        var resultado = validadores.validar(solicitud, pendientes);
        if (!resultado.valida()) {
            throw new SolicitudInvalidaException(resultado.motivoRechazo());
        }
        // Una solicitud nueva nunca reutiliza un id enviado por el cliente
        solicitud.setId(null);
        solicitud.setMontoAprobado(calculadoras.obtener(solicitud.getCategoria()).calcular(solicitud));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        return repositorio.save(solicitud);
    }

    /**
     * Cambia el estado de una solicitud y avisa a todos los interesados.
     *
     * @throws IllegalArgumentException si la solicitud no existe
     */
    public SolicitudReembolso cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        var solicitud = repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + id));
        var estadoAnterior = solicitud.getEstado();
        solicitud.setEstado(nuevoEstado);
        var guardada = repositorio.save(solicitud);
        observadores.forEach(o -> o.onCambioEstado(guardada, estadoAnterior));
        return guardada;
    }
}
