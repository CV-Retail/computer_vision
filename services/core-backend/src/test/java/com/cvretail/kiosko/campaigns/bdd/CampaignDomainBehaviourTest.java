package com.cvretail.kiosko.campaigns.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Behavior of the campaign domain, written as Gherkin scenarios (spec KIO-24, R21). */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.campaigns")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.cvretail.kiosko.campaigns.bdd")
class CampaignDomainBehaviourTest {
}
