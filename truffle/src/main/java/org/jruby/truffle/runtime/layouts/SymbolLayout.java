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
import org.jruby.util.ByteList;

@org.jruby.truffle.om.dsl.api.Layout
public interface SymbolLayout extends BasicObjectLayout {

    DynamicObjectFactory createSymbolShape(DynamicObject logicalClass, DynamicObject metaClass);

    DynamicObject createSymbol(DynamicObjectFactory factory, String string, ByteList byteList, int hashCode,
                               int codeRange, @Nullable SymbolCodeRangeableWrapper codeRangeableWrapper);

    boolean isSymbol(DynamicObject object);

    String getString(DynamicObject object);

    ByteList getByteList(DynamicObject object);

    int getHashCode(DynamicObject object);

    int getCodeRange(DynamicObject object);

    void setCodeRange(DynamicObject object, int codeRange);

    SymbolCodeRangeableWrapper getCodeRangeableWrapper(DynamicObject object);

    void setCodeRangeableWrapper(DynamicObject object, SymbolCodeRangeableWrapper codeRangeableWrapper);

}
