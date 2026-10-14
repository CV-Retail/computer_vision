package com.cvretail.archfixtures.layering.domain.valueobject;

import com.cvretail.archfixtures.layering.domain.entity.SomeEntity;

/** Fixture: a value object that depends on an entity, which the layering rule forbids. */
public class BadValueObject {
    SomeEntity entity;
}
