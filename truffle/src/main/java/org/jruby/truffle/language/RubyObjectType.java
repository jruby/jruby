/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.interop.RubyMessageResolutionAccessor;

public class RubyObjectType extends ObjectType {

    @Override
    public String toString(DynamicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        if (RubyGuards.isRubyString(object)) {
            return RopeOperations.decodeRope(getContext().getJRubyRuntime(), StringOperations.rope(object));
        } else if (RubyGuards.isRubySymbol(object)) {
            return Layouts.SYMBOL.getString(object);
        } else if (RubyGuards.isRubyException(object)) {
            return Layouts.EXCEPTION.getMessage(object).toString();
        } else if (RubyGuards.isRubyModule(object)) {
            return Layouts.MODULE.getFields(object).toString();
        } else {
            return String.format("DynamicObject@%x<logicalClass=%s>", System.identityHashCode(object),
                    Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(object)).getName());
        }
    }

    @Override
    public ForeignAccess getForeignAccessFactory(DynamicObject object) {
        return RubyMessageResolutionAccessor.ACCESS;
    }

    public static boolean isInstance(TruffleObject object) {
        return RubyGuards.isRubyBasicObject(object);
    }

    private RubyContext getContext() {
        return Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(this)).getContext();
    }

}
