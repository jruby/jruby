/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.symbol;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.jruby.truffle.core.rope.Rope;

@Layout
public interface SymbolLayout extends BasicObjectLayout {

    DynamicObjectFactory createSymbolShape(
            DynamicObject logicalClass,
            DynamicObject metaClass);

    DynamicObject createSymbol(
            DynamicObjectFactory factory,
            String string,
            Rope rope,
            int hashCode,
            SymbolEquality equalityWrapper);

    boolean isSymbol(Object object);
    boolean isSymbol(DynamicObject object);

    String getString(DynamicObject object);

    Rope getRope(DynamicObject object);

    int getHashCode(DynamicObject object);

    SymbolEquality getEqualityWrapper(DynamicObject object);

}
