package com.cvretail.kiosko;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Unit test: proves the architecture rules fail on bad code, even while the real layers are empty.
 * The fixtures live outside the application package, so Spring and Modulith ignore them.
 */
class ArchitectureRulesDetectViolationsTest {

    private final JavaClasses fixtures =
            new ClassFileImporter().importPackages("com.cvretail.archfixtures");

    @Test
    void domainRuleFailsWhenDomainUsesAFramework() {
        assertFailsNaming(ArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE, "BadDomainService");
    }

    @Test
    void applicationRuleFailsWhenApplicationUsesInfrastructure() {
        assertFailsNaming(ArchitectureRules.APPLICATION_DOES_NOT_USE_INFRASTRUCTURE, "BadUseCase");
    }

    @Test
    void infrastructureRuleFailsWhenAStrayClassUsesInfrastructure() {
        assertFailsNaming(ArchitectureRules.INFRASTRUCTURE_IS_A_LEAF, "StrayClass");
    }

    private void assertFailsNaming(ArchRule rule, String offendingClass) {
        assertThatThrownBy(() -> rule.check(fixtures))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(offendingClass);
    }
}
