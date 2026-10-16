package com.cvretail.archfixtures.modules.alpha.domain;

import com.cvretail.archfixtures.modules.beta.BetaThing;

/** Fixture: a domain class that uses another module, which the independence rule forbids. */
public class AlphaDomain {
    BetaThing other;
}
