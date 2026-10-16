package com.cvretail.archfixtures.layering.domain.event;

import com.cvretail.archfixtures.layering.domain.entity.SomeEntity;

/** Fixture: an event that depends on an entity, which the layering rule forbids. */
public class BadEvent {
    SomeEntity entity;
}
