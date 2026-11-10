package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class ValidadorMontoMaximoTest {

    private final ValidadorMontoMaximo validador = new ValidadorMontoMaximo();

    @ParameterizedTest
    @CsvSource({
            "VIAJE, 150000",
            "REPRESENTACION, 2000000",
            "CAPACITACION, 3000000"
    })
    void apruebaUnMontoIgualAlTopeDeLaCategoria(CategoriaGasto categoria, String monto) {
        var resultado = validador.validar(solicitud(categoria, monto), List.of());

        assertThat(resultado.valida()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "VIAJE, 150001",
            "REPRESENTACION, 2000001",
            "CAPACITACION, 3000001"
    })
    void rechazaUnMontoQueExcedeElTopeDeLaCategoria(CategoriaGasto categoria, String monto) {
        var resultado = validador.validar(solicitud(categoria, monto), List.of());

        assertThat(resultado.valida()).isFalse();
        assertThat(resultado.motivoRechazo())
                .isEqualTo("Monto excede el tope de la categoria " + categoria);
    }

    @Test
    void cadaCategoriaTieneSuPropioTope() {
        // 500.000 excede el tope de VIAJE pero no el de REPRESENTACION
        assertThat(validador.validar(solicitud(CategoriaGasto.VIAJE, "500000"), List.of()).valida())
                .isFalse();
        assertThat(validador.validar(solicitud(CategoriaGasto.REPRESENTACION, "500000"), List.of()).valida())
                .isTrue();
    }
}
