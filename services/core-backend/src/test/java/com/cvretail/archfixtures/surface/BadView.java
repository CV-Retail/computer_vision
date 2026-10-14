package com.cvretail.archfixtures.surface;

import com.cvretail.archfixtures.surface.domain.InternalType;

/** Fixture: a published type that exposes an internal one, which the surface rule forbids. */
public class BadView {
    InternalType internal;
}
