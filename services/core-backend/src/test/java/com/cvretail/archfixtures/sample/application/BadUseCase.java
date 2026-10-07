package com.cvretail.archfixtures.sample.application;

import com.cvretail.archfixtures.sample.infrastructure.SomeAdapter;

/** Deliberately bad: an application class that depends on infrastructure. */
public class BadUseCase {

    private SomeAdapter adapter;
}
