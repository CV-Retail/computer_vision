package com.cvretail.archfixtures.sample.domain;

import org.springframework.context.ApplicationContext;

/** Deliberately bad: a domain class that depends on Spring. */
public class BadDomainService {

    private ApplicationContext context;
}
