package com.cvretail.archfixtures.sample;

import com.cvretail.archfixtures.sample.infrastructure.SomeAdapter;

/** Deliberately bad: a class outside infrastructure that depends on infrastructure. */
public class StrayClass {

    private SomeAdapter adapter;
}
