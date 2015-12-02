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
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.ArrayForeignAccessFactory;
import org.jruby.truffle.runtime.core.BasicForeignAccessFactory;
import org.jruby.truffle.runtime.core.HashForeignAccessFactory;
import org.jruby.truffle.runtime.core.StringForeignAccessFactory;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;

public class RubyObjectType extends ObjectType {

    @Override
    public String toString(DynamicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        if (RubyGuards.isRubyString(object)) {
            return Helpers.decodeByteList(getContext().getRuntime(), StringOperations.getByteList(object));
        } else if (RubyGuards.isRubySymbol(object)) {
            return Layouts.SYMBOL.getString(object);
        } else if (RubyGuards.isRubyException(object)) {
            return Layouts.EXCEPTION.getMessage(object).toString();
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
        } else if (Layouts.PROC.isProc(this)) {
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
        return Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(this)).getContext();
    }

}
