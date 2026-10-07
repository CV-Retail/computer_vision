package com.cvretail.kiosko;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Clean Architecture rules of the backend (spec KIO-7, R12). The layers are empty in the skeleton,
 * so the rules allow an empty selection; the fixtures test proves they fail on bad code.
 */
final class ArchitectureRules {

    static final String ROOT_PACKAGE = "com.cvretail.kiosko";

    /** Domain depends on nothing but the Java standard library. */
    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..",
                    "..infrastructure..",
                    "org.springframework..",
                    "jakarta..",
                    "javax..",
                    "org.flywaydb..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "java.sql..")
            .because("the domain layer must be free of frameworks and of other layers")
            .allowEmptyShould(true);

    /** Application never depends on infrastructure. */
    static final ArchRule APPLICATION_DOES_NOT_USE_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("use cases depend on ports, not on adapters")
            .allowEmptyShould(true);

    /** Nothing depends on infrastructure, except the Spring wiring in the root package. */
    static final ArchRule INFRASTRUCTURE_IS_A_LEAF = noClasses()
            .that().resideOutsideOfPackages("..infrastructure..", ROOT_PACKAGE)
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("only the Spring wiring may reference adapters")
            .allowEmptyShould(true);

    private ArchitectureRules() {
    }
}
