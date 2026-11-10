package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

class ValidadorDuplicadosTest {

    private final ValidadorDuplicados validador = new ValidadorDuplicados();

    @Test
    void apruebaCuandoElEmpleadoNoTienePendientes() {
        var resultado = validador.validar(solicitud(CategoriaGasto.VIAJE, "100000"), List.of());

        assertThat(resultado.valida()).isTrue();
    }

    @Test
    void rechazaCuandoYaHayUnaPendienteDeLaMismaCategoriaEnElMismoPeriodo() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "80000");
        pendiente.setFechaGasto(LocalDate.of(2026, 11, 20));
        var nueva = solicitud(CategoriaGasto.VIAJE, "100000");
        nueva.setFechaGasto(LocalDate.of(2026, 11, 5));

        var resultado = validador.validar(nueva, List.of(pendiente));

        assertThat(resultado.valida()).isFalse();
        assertThat(resultado.motivoRechazo())
                .isEqualTo("Ya existe una solicitud pendiente de la misma categoria en el periodo");
    }

    @Test
    void apruebaCuandoLaPendienteEsDeOtraCategoria() {
        var pendiente = solicitud(CategoriaGasto.CAPACITACION, "80000");

        var resultado = validador.validar(solicitud(CategoriaGasto.VIAJE, "100000"), List.of(pendiente));

        assertThat(resultado.valida()).isTrue();
    }

    @Test
    void apruebaCuandoLaPendienteEsDeOtroMes() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "80000");
        pendiente.setFechaGasto(LocalDate.of(2026, 10, 5));

        var resultado = validador.validar(solicitud(CategoriaGasto.VIAJE, "100000"), List.of(pendiente));

        assertThat(resultado.valida()).isTrue();
    }

    @Test
    void apruebaCuandoLaPendienteEsDelMismoMesPeroDeOtroAnio() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "80000");
        pendiente.setFechaGasto(LocalDate.of(2025, 11, 5));

        var resultado = validador.validar(solicitud(CategoriaGasto.VIAJE, "100000"), List.of(pendiente));

        assertThat(resultado.valida()).isTrue();
    }
}
