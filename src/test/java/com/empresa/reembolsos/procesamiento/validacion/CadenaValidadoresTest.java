package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class CadenaValidadoresTest {

    private final CadenaValidadores cadena = new CadenaValidadores(
            new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(), new ValidadorDuplicados());

    @Test
    void apruebaUnaSolicitudQuePasaLasTresVerificaciones() {
        var resultado = cadena.validar(solicitud(CategoriaGasto.VIAJE, "100000"), List.of());

        assertThat(resultado.valida()).isTrue();
    }

    @Test
    void seDetieneEnElPrimerRechazoSiguiendoElOrdenDocumentosMontoDuplicados() {
        // Falla las tres verificaciones a la vez: sin documentos, monto excedido y duplicada
        var pendiente = solicitud(CategoriaGasto.VIAJE, "80000");
        var nueva = solicitud(CategoriaGasto.VIAJE, "999999");
        nueva.setDocumentosSoporte(false);

        assertThat(cadena.validar(nueva, List.of(pendiente)).motivoRechazo())
                .isEqualTo("Faltan documentos soporte");

        nueva.setDocumentosSoporte(true);
        assertThat(cadena.validar(nueva, List.of(pendiente)).motivoRechazo())
                .isEqualTo("Monto excede el tope de la categoria VIAJE");

        nueva.setMontoSolicitado(pendiente.getMontoSolicitado());
        assertThat(cadena.validar(nueva, List.of(pendiente)).motivoRechazo())
                .isEqualTo("Ya existe una solicitud pendiente de la misma categoria en el periodo");
    }
}
