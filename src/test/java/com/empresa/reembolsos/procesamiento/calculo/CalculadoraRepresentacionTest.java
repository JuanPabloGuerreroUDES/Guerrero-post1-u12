package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraRepresentacionTest {

    private final CalculadoraRepresentacion calculadora = new CalculadoraRepresentacion();

    @Test
    void atiendeLaCategoriaRepresentacion() {
        assertThat(calculadora.getCategoria()).isEqualTo(CategoriaGasto.REPRESENTACION);
    }

    @Test
    void reembolsaElMontoCompletoSiNoSuperaElTopeMensual() {
        var solicitud = solicitud(CategoriaGasto.REPRESENTACION, "500000");

        assertThat(calculadora.calcular(solicitud)).isEqualByComparingTo("500000");
    }

    @Test
    void recortaAlTopeMensual() {
        var solicitud = solicitud(CategoriaGasto.REPRESENTACION, "2500000");

        assertThat(calculadora.calcular(solicitud)).isEqualByComparingTo("2000000");
    }
}
