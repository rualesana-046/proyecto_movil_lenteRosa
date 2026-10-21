package com.lenterosa.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// Reglas de capas dentro de cada módulo. Sin allowEmptyShould: si una regla no encontrara
// clases que verificar, ArchUnit la daría por fallida en vez de aprobarla en silencio.
@AnalyzeClasses(packages = "com.lenterosa.api", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothingElse = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..", "org.springframework..", "jakarta..")
            .because("el dominio no depende de casos de uso, infraestructura, Spring ni JPA");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("los casos de uso no dependen de la infraestructura");
}
