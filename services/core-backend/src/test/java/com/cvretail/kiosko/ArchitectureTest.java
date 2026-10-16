package com.cvretail.kiosko;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Unit test: ArchUnit rules over the production classes. */
class ArchitectureTest {

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ArchitectureRules.ROOT_PACKAGE);

    @Test
    void domainIsFrameworkFree() {
        ArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE.check(production);
    }

    @Test
    void applicationDoesNotUseInfrastructure() {
        ArchitectureRules.APPLICATION_DOES_NOT_USE_INFRASTRUCTURE.check(production);
    }

    @Test
    void infrastructureIsALeaf() {
        ArchitectureRules.INFRASTRUCTURE_IS_A_LEAF.check(production);
    }

    @Test
    void publishedSurfaceIsSelfContained() {
        ArchitectureRules.PUBLISHED_SURFACE_IS_SELF_CONTAINED.check(production);
    }

    @Test
    void domainBuildingBlocksDependOnlyOnTheAllowedOnes() {
        ArchitectureRules.DOMAIN_INTERNAL_LAYERING.check(production);
    }

    @Test
    void noClassReadsTheSystemClockOrZone() {
        ArchitectureRules.NO_SYSTEM_CLOCK_ACCESS.check(production);
    }

    @Test
    void domainsAreIndependentOfTheOtherModules() {
        ArchitectureRules.CAMPAIGNS_DOMAIN_IS_INDEPENDENT.check(production);
        ArchitectureRules.RULES_DOMAIN_IS_INDEPENDENT.check(production);
        ArchitectureRules.REPORTS_DOMAIN_IS_INDEPENDENT.check(production);
    }

    @Test
    void rulesPublishesNothingYet() {
        long classesInTheBasePackage = production.stream()
                .filter(c -> c.getPackageName().equals(ArchitectureRules.ROOT_PACKAGE + ".rules"))
                .filter(c -> !c.getSimpleName().equals("package-info"))
                .count();
        org.assertj.core.api.Assertions.assertThat(classesInTheBasePackage).isZero();
    }
}
