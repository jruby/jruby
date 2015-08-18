/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.util.Arrays;

public class RubyObjectType extends ObjectType {

    @Override
    public String toString(DynamicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        final RubyContext context = getContext();

        if (RubyGuards.isRubyString(object)) {
            return Helpers.decodeByteList(context.getRuntime(), Layouts.STRING.getByteList(object));
        } else if (RubyGuards.isRubySymbol(object)) {
            return Layouts.SYMBOL.getString(object);
        } else if (RubyGuards.isRubyException(object)) {
            return Layouts.EXCEPTION.getMessage(object) + " :\n" +
                    Arrays.toString(Backtrace.EXCEPTION_FORMATTER.format(context, object, Layouts.EXCEPTION.getBacktrace(object)));
        } else if (RubyGuards.isRubyModule(object)) {
            return Layouts.MODULE.getFields(object).toString();
        } else {
            return String.format("DynamicObject@%x<logicalClass=%s>", System.identityHashCode(object), Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(object)).getName());
        }
    }

    @Override
    public ForeignAccess getForeignAccessFactory() {
        if (Layouts.METHOD.isMethod(this)) {
            return RubyMethodForeignAccessFactory.create(getContext());
        } else if (Layouts.ARRAY.isArray(this)) {
            return ArrayForeignAccessFactory.create(getContext());
        } else if (Layouts.HASH.isHash(this)) {
            return HashForeignAccessFactory.create(getContext());
        } else if (Layouts.STRING.isString(this)) {
            return StringForeignAccessFactory.create(getContext());
        } else {
            return BasicForeignAccessFactory.create(getContext());
        }
    }

    private RubyContext getContext() {
        return Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(Layouts.BASIC_OBJECT.getLogicalClass(this))).getContext();
    }

}
