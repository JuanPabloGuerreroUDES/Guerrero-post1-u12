package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;

/**
 * Estrategia de calculo del monto aprobado (Strategy, RF2).
 *
 * <p>Cada categoria de gasto tiene su propia implementacion con su formula.
 * Agregar una categoria nueva consiste en crear otra implementacion anotada
 * con {@code @Component}; no se modifica ninguna de las existentes.</p>
 */
public interface CalculadoraReembolso {

    /**
     * @return la categoria de gasto que esta calculadora sabe resolver
     */
    CategoriaGasto getCategoria();

    /**
     * Calcula el monto que se aprueba para la solicitud segun la formula de
     * la categoria.
     *
     * @param solicitud solicitud ya validada
     * @return monto aprobado, nunca mayor al monto solicitado
     */
    BigDecimal calcular(SolicitudReembolso solicitud);
}
