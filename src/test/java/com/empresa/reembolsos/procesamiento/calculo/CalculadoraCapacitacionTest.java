package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraCapacitacionTest {

    private final CalculadoraCapacitacion calculadora = new CalculadoraCapacitacion();

    @Test
    void atiendeLaCategoriaCapacitacion() {
        assertThat(calculadora.getCategoria()).isEqualTo(CategoriaGasto.CAPACITACION);
    }

    @Test
    void reembolsaElOchentaPorCientoDelMonto() {
        var solicitud = solicitud(CategoriaGasto.CAPACITACION, "1000000");

        assertThat(calculadora.calcular(solicitud)).isEqualByComparingTo("800000");
    }

    @Test
    void recortaAlTopeAnualCuandoElOchentaPorCientoLoSupera() {
        var solicitud = solicitud(CategoriaGasto.CAPACITACION, "5000000"); // 80% = 4.000.000

        assertThat(calculadora.calcular(solicitud)).isEqualByComparingTo("3000000");
    }
}
