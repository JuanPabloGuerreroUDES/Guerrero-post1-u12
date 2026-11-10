package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraViajeTest {

    private final CalculadoraViaje calculadora = new CalculadoraViaje();

    @Test
    void atiendeLaCategoriaViaje() {
        assertThat(calculadora.getCategoria()).isEqualTo(CategoriaGasto.VIAJE);
    }

    @Test
    void reembolsaElMontoCompletoSiNoSuperaElTopeDiarioPorLosDiasDeViaje() {
        var solicitud = solicitud(CategoriaGasto.VIAJE, "400000");
        solicitud.setDiasViaje(3); // tope: 150.000 x 3 = 450.000

        assertThat(calculadora.calcular(solicitud)).isEqualByComparingTo("400000");
    }

    @Test
    void recortaAlTopeDiarioMultiplicadoPorLosDiasDeViaje() {
        var solicitud = solicitud(CategoriaGasto.VIAJE, "400000");
        solicitud.setDiasViaje(2); // tope: 150.000 x 2 = 300.000

        assertThat(calculadora.calcular(solicitud)).isEqualByComparingTo("300000");
    }
}
