package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificadoresTest {

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = {"APROBADA", "RECHAZADA", "REQUIERE_CORRECCION"})
    void elEmpleadoRecibeElResultadoDeCualquierCambioDeEstado(EstadoSolicitud nuevoEstado, CapturedOutput salida) {
        new NotificadorEmpleado().onCambioEstado(solicitudEn(nuevoEstado), EstadoSolicitud.PENDIENTE);

        assertThat(salida.getOut())
                .contains("Notificando a EMP-001: su solicitud paso de PENDIENTE a " + nuevoEstado);
    }

    @Test
    void contabilidadProgramaElPagoCuandoLaSolicitudSeAprueba(CapturedOutput salida) {
        new NotificadorContabilidad().onCambioEstado(solicitudEn(EstadoSolicitud.APROBADA), EstadoSolicitud.PENDIENTE);

        assertThat(salida.getOut()).contains("Contabilidad: programar pago de 100000 para EMP-001");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = {"RECHAZADA", "REQUIERE_CORRECCION", "PENDIENTE"})
    void contabilidadIgnoraLosCambiosQueNoSonAprobaciones(EstadoSolicitud nuevoEstado, CapturedOutput salida) {
        new NotificadorContabilidad().onCambioEstado(solicitudEn(nuevoEstado), EstadoSolicitud.PENDIENTE);

        assertThat(salida.getOut()).doesNotContain("Contabilidad");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = {"APROBADA", "RECHAZADA", "REQUIERE_CORRECCION"})
    void auditoriaRegistraTodoCambioDeEstado(EstadoSolicitud nuevoEstado, CapturedOutput salida) {
        new NotificadorAuditoria().onCambioEstado(solicitudEn(nuevoEstado), EstadoSolicitud.PENDIENTE);

        assertThat(salida.getAll())
                .contains("Auditoria: solicitud 7 cambio de PENDIENTE a " + nuevoEstado);
    }

    private static SolicitudReembolso solicitudEn(EstadoSolicitud estado) {
        var solicitud = solicitud(CategoriaGasto.VIAJE, "100000");
        solicitud.setId(7L);
        solicitud.setMontoAprobado(new BigDecimal("100000"));
        solicitud.setEstado(estado);
        return solicitud;
    }
}
