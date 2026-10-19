package com.lenterosa.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// Reglas de capas dentro de cada módulo. allowEmptyShould permite que pasen mientras
// los módulos aún no tengan clases; en cuanto existan, las reglas se aplican.
@AnalyzeClasses(packages = "com.lenterosa.api", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothingElse = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..", "org.springframework..")
            .because("el dominio no depende de casos de uso, infraestructura ni Spring")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("los casos de uso no dependen de la infraestructura")
            .allowEmptyShould(true);
}
