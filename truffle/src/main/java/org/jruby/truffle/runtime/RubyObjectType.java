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
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.*;

import java.util.Arrays;

public class RubyObjectType extends ObjectType {

    @Override
    public String toString(DynamicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        final RubyContext context = getContext();

        if (RubyGuards.isRubyString(object)) {
            return Helpers.decodeByteList(context.getRuntime(), StringNodes.getByteList(object));
        } else if (RubyGuards.isRubySymbol(object)) {
            return SymbolNodes.getString(object);
        } else if (RubyGuards.isRubyException(object)) {
            return ExceptionNodes.getMessage(object) + " :\n" +
                    Arrays.toString(Backtrace.EXCEPTION_FORMATTER.format(context, object, ExceptionNodes.getBacktrace(object)));
        } else if (RubyGuards.isRubyModule(object)) {
            return ModuleNodes.getFields(object).toString();
        } else {
            return String.format("DynamicObject@%x<logicalClass=%s>", System.identityHashCode(object), ModuleNodes.getFields(BasicObjectNodes.getLogicalClass(object)).getName());
        }
    }

    @Override
    public ForeignAccess getForeignAccessFactory() {
        if (MethodNodes.METHOD_LAYOUT.isMethod(this)) {
            return RubyMethodForeignAccessFactory.create(getContext());
        } else if (ArrayNodes.ARRAY_LAYOUT.isArray(this)) {
            return ArrayForeignAccessFactory.create(getContext());
        } else if (HashNodes.HASH_LAYOUT.isHash(this)) {
            return HashForeignAccessFactory.create(getContext());
        } else if (StringNodes.STRING_LAYOUT.isString(this)) {
            return StringForeignAccessFactory.create(getContext());
        } else {
            return BasicForeignAccessFactory.create(getContext());
        }
    }

    private RubyContext getContext() {
        return BasicObjectNodes.getContext(BasicObjectNodes.BASIC_OBJECT_LAYOUT.getLogicalClass(this));
    }

}
