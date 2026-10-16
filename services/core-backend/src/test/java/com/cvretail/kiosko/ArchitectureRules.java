package com.cvretail.kiosko;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

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

    private static DescribedPredicate<JavaCall<?>> noArgumentCall(String method, Class<?>... owners) {
        return target(name(method)).and(target(rawParameterTypes(new Class<?>[0]))).and(target(owner(belongToAnyOf(owners))));
    }

    private static final DescribedPredicate<JavaMethodCall> READS_THE_SYSTEM_CLOCK_OR_ZONE =
            DescribedPredicate.describe("read the system clock or the default time zone", call ->
                    noArgumentCall("now", Instant.class, LocalDate.class, LocalTime.class, LocalDateTime.class,
                            ZonedDateTime.class, OffsetDateTime.class, OffsetTime.class, Year.class, YearMonth.class)
                            .test(call)
                    || noArgumentCall("systemDefault", ZoneId.class).test(call)
                    || noArgumentCall("getDefault", TimeZone.class).test(call)
                    || noArgumentCall("currentTimeMillis", System.class).test(call));

    /**
     * Time comes from an injected {@code Clock} (spec KIO-25, R7.2): no production class reads the system clock
     * or the default time zone by itself. The overloads that take a clock are allowed.
     */
    static final ArchRule NO_SYSTEM_CLOCK_ACCESS = noClasses()
            .should().callMethodWhere(READS_THE_SYSTEM_CLOCK_OR_ZONE)
            .because("time and zone must come from an injected Clock so tests can fix them")
            .allowEmptyShould(true);

    /** A domain package does not use other modules at all (spec KIO-25, R9.1). */
    static ArchRule domainIsIndependentOf(String domainPackage, String... forbiddenPackages) {
        return noClasses()
                .that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages)
                .because("a domain is independent of the other modules; the application layer translates")
                .allowEmptyShould(true);
    }

    static final ArchRule CAMPAIGNS_DOMAIN_IS_INDEPENDENT = domainIsIndependentOf(
            ROOT_PACKAGE + ".campaigns.domain..", ROOT_PACKAGE + ".rules..", ROOT_PACKAGE + ".reports..");

    static final ArchRule RULES_DOMAIN_IS_INDEPENDENT = domainIsIndependentOf(
            ROOT_PACKAGE + ".rules.domain..", ROOT_PACKAGE + ".campaigns..", ROOT_PACKAGE + ".reports..");

    static final ArchRule REPORTS_DOMAIN_IS_INDEPENDENT = domainIsIndependentOf(
            ROOT_PACKAGE + ".reports.domain..", ROOT_PACKAGE + ".campaigns..", ROOT_PACKAGE + ".rules..");

    private ArchitectureRules() {
    }
}
