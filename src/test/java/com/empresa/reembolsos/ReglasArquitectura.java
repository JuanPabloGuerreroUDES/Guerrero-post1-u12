package com.empresa.reembolsos;

import com.empresa.reembolsos.procesamiento.calculo.CalculadoraReembolso;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de arquitectura del sistema de reembolsos.
 *
 * <p>Verifican los limites que si existen entre paquetes. Ninguna regla
 * asume un puerto de persistencia, en coherencia con ADR-001. Solo se
 * analizan las clases de produccion.</p>
 */
@AnalyzeClasses(packages = "com.empresa.reembolsos", importOptions = ImportOption.DoNotIncludeTests.class)
public class ReglasArquitectura {

    // Regla 1: procesamiento no depende de infraestructura, adaptadores ni JPA
    @ArchTest
    static final ArchRule procesamientoSinPersistenciaNiWeb = noClasses()
            .that().resideInAPackage("..procesamiento..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..infraestructura..", "..adaptadores..",
                    "org.springframework.data..", "jakarta.persistence..");

    // Regla 2: el controlador solo accede al servicio de aplicacion y al dominio
    // (se incluye su propio paquete porque ArchUnit cuenta como acceso la
    // lectura y escritura del campo privado "servicio" del controlador)
    @ArchTest
    static final ArchRule controladorSoloUsaServicio = classes()
            .that().resideInAPackage("..adaptadores.rest..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                    "..adaptadores.rest..", "..aplicacion..", "..dominio..",
                    "org.springframework.web..", "org.springframework.http..", "java..");

    // Regla 3: las calculadoras implementan CalculadoraReembolso
    // (SelectorCalculadora vive en el mismo paquete pero no es una estrategia:
    // por eso la regla se limita a las clases cuyo nombre empieza por Calculadora)
    @ArchTest
    static final ArchRule calculadorasImplementanInterfaz = classes()
            .that().resideInAPackage("..procesamiento.calculo..")
            .and().areNotInterfaces()
            .and().haveSimpleNameStartingWith("Calculadora")
            .should().implement(CalculadoraReembolso.class);

    // Regla 4: los notificadores implementan ObservadorCambioEstado
    @ArchTest
    static final ArchRule notificadoresImplementanInterfaz = classes()
            .that().resideInAPackage("..procesamiento.notificacion..")
            .and().areNotInterfaces()
            .should().implement(ObservadorCambioEstado.class);
}
