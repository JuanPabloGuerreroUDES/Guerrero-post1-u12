package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class ValidadorDocumentosSoporteTest {

    private final ValidadorDocumentosSoporte validador = new ValidadorDocumentosSoporte();

    @Test
    void apruebaCuandoLaSolicitudTieneDocumentosSoporte() {
        var solicitud = solicitud(CategoriaGasto.VIAJE, "100000");

        var resultado = validador.validar(solicitud, List.of());

        assertThat(resultado.valida()).isTrue();
        assertThat(resultado.motivoRechazo()).isNull();
    }

    @Test
    void rechazaCuandoFaltanLosDocumentosSoporte() {
        var solicitud = solicitud(CategoriaGasto.VIAJE, "100000");
        solicitud.setDocumentosSoporte(false);

        var resultado = validador.validar(solicitud, List.of());

        assertThat(resultado.valida()).isFalse();
        assertThat(resultado.motivoRechazo()).isEqualTo("Faltan documentos soporte");
    }
}
