/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.*;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.*;

/**
 * Represents the Ruby {@code BasicObject} class - the root of the Ruby class hierarchy.
 */
public class RubyBasicObject implements TruffleObject {

    public final DynamicObject dynamicObject;

    /** The class of the object, not a singleton class. */
    @CompilationFinal public RubyBasicObject logicalClass;
    /** Either the singleton class if it exists or the logicalClass. */
    @CompilationFinal public RubyBasicObject metaClass;

    public RubyBasicObject(RubyBasicObject rubyClass, DynamicObject dynamicObject) {
        assert rubyClass == null || RubyGuards.isRubyClass(rubyClass);

        this.dynamicObject = dynamicObject;

        if (rubyClass == null && RubyGuards.isRubyClass(this)) { // For class Class
            rubyClass = this;
        }
        BasicObjectNodes.unsafeSetLogicalClass(this, rubyClass);
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return BasicObjectNodes.getForeignAccessFactory(this);
    }

    @Override
    public String toString() {
        return BasicObjectNodes.toString(this);
    }

}
