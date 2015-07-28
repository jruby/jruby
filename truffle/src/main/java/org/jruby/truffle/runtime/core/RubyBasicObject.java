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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.*;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.runtime.backtrace.Backtrace;

import java.util.Arrays;

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
        if (RubyGuards.isRubyArray(this)) {
            return new ArrayForeignAccessFactory(BasicObjectNodes.getContext(this));
        } else if (RubyGuards.isRubyHash(this)) {
            return new HashForeignAccessFactory(BasicObjectNodes.getContext(this));
        } else if (RubyGuards.isRubyString(this)) {
            return new StringForeignAccessFactory(BasicObjectNodes.getContext(this));
        } else {
            return new BasicForeignAccessFactory(BasicObjectNodes.getContext(this));
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation("RubyBasicObject#toString should only be used for debugging");

        if (RubyGuards.isRubyString(this)) {
            return Helpers.decodeByteList(BasicObjectNodes.getContext(this).getRuntime(), StringNodes.getByteList((this)));
        } else if (RubyGuards.isRubySymbol(this)) {
            return SymbolNodes.getString(this);
        } else if (RubyGuards.isRubyException(this)) {
            return ExceptionNodes.getMessage(this) + " : " + super.toString() + "\n" +
                    Arrays.toString(Backtrace.EXCEPTION_FORMATTER.format(BasicObjectNodes.getContext(this), this, ExceptionNodes.getBacktrace(this)));
        } else if (RubyGuards.isRubyModule(this)) {
            return ModuleNodes.getModel(this).toString();
        } else {
            return String.format("RubyBasicObject@%x<logicalClass=%s>", System.identityHashCode(this), ModuleNodes.getModel(logicalClass).getName());
        }
    }

}
