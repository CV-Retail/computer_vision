package com.cvretail.kiosko;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

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

    /**
     * The published surface of a module (classes directly in its base package) is self-contained: it uses no
     * internal package and no framework (spec KIO-24, R15.2).
     */
    static ArchRule publishedSurfaceIsSelfContained(String modulePackages) {
        return noClasses()
                .that().resideInAPackage(modulePackages)
                .and().haveSimpleNameNotContaining("package-info")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..domain..", "..application..", "..infrastructure..", "org.springframework..")
                .because("other modules read the surface and must not be coupled to the internals")
                .allowEmptyShould(true);
    }

    static final ArchRule PUBLISHED_SURFACE_IS_SELF_CONTAINED = publishedSurfaceIsSelfContained(ROOT_PACKAGE + ".*");

    /**
     * Dependencies between the building blocks of a domain (spec KIO-24, R19.3): exception depends on nothing;
     * valueobject on exception; event on valueobject; service on valueobject and exception; entity on
     * valueobject, event, exception and service; repository on entity and valueobject. Generic for every module.
     */
    static final ArchRule DOMAIN_INTERNAL_LAYERING = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Exception").definedBy("..domain.exception..")
            .layer("ValueObject").definedBy("..domain.valueobject..")
            .layer("Event").definedBy("..domain.event..")
            .layer("Service").definedBy("..domain.service..")
            .layer("Entity").definedBy("..domain.entity..")
            .layer("Repository").definedBy("..domain.repository..")
            .whereLayer("Repository").mayNotBeAccessedByAnyLayer()
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Repository")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Entity")
            .whereLayer("Event").mayOnlyBeAccessedByLayers("Entity")
            .whereLayer("ValueObject").mayOnlyBeAccessedByLayers("Event", "Service", "Entity", "Repository")
            .whereLayer("Exception").mayOnlyBeAccessedByLayers("ValueObject", "Service", "Entity")
            .allowEmptyShould(true);

    private ArchitectureRules() {
    }
}
