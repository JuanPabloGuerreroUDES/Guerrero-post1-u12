package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.empresa.reembolsos.SolicitudesDePrueba.solicitud;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion del flujo completo por HTTP: crear la solicitud,
 * validarla en todas sus etapas, calcular el monto aprobado segun la
 * categoria y notificar a los tres interesados ante un cambio de estado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class FlujoSolicitudReembolsoIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void creaSolicitudesDeLasTresCategoriasConSuMontoAprobado() throws Exception {
        var viaje = solicitud(CategoriaGasto.VIAJE, "120000");
        viaje.setEmpleadoId("EMP-CATEGORIAS");
        viaje.setDiasViaje(2);
        var representacion = solicitud(CategoriaGasto.REPRESENTACION, "500000");
        representacion.setEmpleadoId("EMP-CATEGORIAS");
        var capacitacion = solicitud(CategoriaGasto.CAPACITACION, "1000000");
        capacitacion.setEmpleadoId("EMP-CATEGORIAS");

        var viajeCreada = crear(viaje);
        var representacionCreada = crear(representacion);
        var capacitacionCreada = crear(capacitacion);

        assertThat(viajeCreada.getMontoAprobado()).isEqualByComparingTo("120000");
        assertThat(representacionCreada.getMontoAprobado()).isEqualByComparingTo("500000");
        assertThat(capacitacionCreada.getMontoAprobado()).isEqualByComparingTo("800000");
        assertThat(java.util.List.of(viajeCreada, representacionCreada, capacitacionCreada))
                .allSatisfy(creada -> {
                    assertThat(creada.getId()).isNotNull();
                    assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
                });
    }

    @Test
    void rechazaConMensajeClaroUnaSolicitudSinDocumentosSoporte() throws Exception {
        var sinDocumentos = solicitud(CategoriaGasto.REPRESENTACION, "500000");
        sinDocumentos.setEmpleadoId("EMP-SIN-DOCS");
        sinDocumentos.setDocumentosSoporte(false);

        postSolicitud(sinDocumentos)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Faltan documentos soporte"));
    }

    @Test
    void rechazaUnaSolicitudQueExcedeElTopeDeSuCategoria() throws Exception {
        var excedida = solicitud(CategoriaGasto.VIAJE, "150001");
        excedida.setEmpleadoId("EMP-TOPE");

        postSolicitud(excedida)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Monto excede el tope de la categoria VIAJE"));
    }

    @Test
    void rechazaUnaSegundaSolicitudPendienteDeLaMismaCategoriaYPeriodo() throws Exception {
        var primera = solicitud(CategoriaGasto.CAPACITACION, "900000");
        primera.setEmpleadoId("EMP-DUPLICADO");
        var segunda = solicitud(CategoriaGasto.CAPACITACION, "300000");
        segunda.setEmpleadoId("EMP-DUPLICADO");

        crear(primera);

        postSolicitud(segunda)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value("Ya existe una solicitud pendiente de la misma categoria en el periodo"));
    }

    @Test
    void alAprobarSeEnteranLosTresInteresados(CapturedOutput salida) throws Exception {
        var solicitud = solicitud(CategoriaGasto.CAPACITACION, "1000000");
        solicitud.setEmpleadoId("EMP-APROBADA");
        var creada = crear(solicitud);

        cambiarEstado(creada.getId(), EstadoSolicitud.APROBADA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        assertThat(salida.getAll())
                .contains("Notificando a EMP-APROBADA: su solicitud paso de PENDIENTE a APROBADA")
                .contains("Contabilidad: programar pago de")
                .contains("para EMP-APROBADA")
                .contains("Auditoria: solicitud " + creada.getId() + " cambio de PENDIENTE a APROBADA");
    }

    @Test
    void alRechazarSeEnteranEmpleadoYAuditoriaPeroNoContabilidad(CapturedOutput salida) throws Exception {
        var solicitud = solicitud(CategoriaGasto.REPRESENTACION, "700000");
        solicitud.setEmpleadoId("EMP-RECHAZADA");
        var creada = crear(solicitud);

        cambiarEstado(creada.getId(), EstadoSolicitud.RECHAZADA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        assertThat(salida.getAll())
                .contains("Notificando a EMP-RECHAZADA: su solicitud paso de PENDIENTE a RECHAZADA")
                .contains("Auditoria: solicitud " + creada.getId() + " cambio de PENDIENTE a RECHAZADA")
                .doesNotContain("para EMP-RECHAZADA");
    }

    @Test
    void respondeConErrorClaroSiLaSolicitudNoExiste() throws Exception {
        cambiarEstado(999_999L, EstadoSolicitud.APROBADA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Solicitud no encontrada: 999999"));
    }

    private SolicitudReembolso crear(SolicitudReembolso solicitud) throws Exception {
        var respuesta = postSolicitud(solicitud)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readValue(respuesta, SolicitudReembolso.class);
    }

    private ResultActions postSolicitud(SolicitudReembolso solicitud) throws Exception {
        return mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(solicitud)));
    }

    private ResultActions cambiarEstado(Long id, EstadoSolicitud nuevoEstado) throws Exception {
        return mockMvc.perform(put("/api/solicitudes/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(nuevoEstado)));
    }
}
