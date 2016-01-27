/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.layouts;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.core.SymbolCodeRangeableWrapper;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.util.ByteList;

@org.jruby.truffle.om.dsl.api.Layout
public interface SymbolLayout extends BasicObjectLayout {

    DynamicObjectFactory createSymbolShape(DynamicObject logicalClass,
                                           DynamicObject metaClass);

    DynamicObject createSymbol(DynamicObjectFactory factory,
                               String string,
                               Rope rope,
                               int hashCode,
                               @Nullable SymbolCodeRangeableWrapper codeRangeableWrapper);

    boolean isSymbol(Object object);
    boolean isSymbol(DynamicObject object);

    String getString(DynamicObject object);

    Rope getRope(DynamicObject object);

    int getHashCode(DynamicObject object);

    SymbolCodeRangeableWrapper getCodeRangeableWrapper(DynamicObject object);
    void setCodeRangeableWrapper(DynamicObject object, SymbolCodeRangeableWrapper codeRangeableWrapper);

}
