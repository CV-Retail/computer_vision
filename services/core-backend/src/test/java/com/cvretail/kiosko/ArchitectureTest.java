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
}
