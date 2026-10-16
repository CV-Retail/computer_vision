package com.cvretail.kiosko.rules.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Behavior of step 1 of the rules engine, schedule and scope, as Gherkin scenarios (spec KIO-25, R11). */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.rules")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.cvretail.kiosko.rules.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber-reports/rules.html")
class RulesBehaviourTest {
}
