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
import com.oracle.truffle.api.object.ObjectType;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.core.StringCodeRangeableWrapper;
import org.jruby.util.ByteList;

@Layout
public interface StringLayout extends BasicObjectLayout {

    DynamicObjectFactory createStringShape(DynamicObject logicalClass,
                                           DynamicObject metaClass);

    DynamicObject createString(DynamicObjectFactory factory,
                               ByteList byteList,
                               int codeRange,
                               @Nullable StringCodeRangeableWrapper codeRangeableWrapper);

    boolean isString(ObjectType objectType);
    boolean isString(DynamicObject dynamicObject);

    ByteList getByteList(DynamicObject object);
    void setByteList(DynamicObject object, ByteList byteList);

    int getCodeRange(DynamicObject object);
    void setCodeRange(DynamicObject object, int codeRange);

    StringCodeRangeableWrapper getCodeRangeableWrapper(DynamicObject object);
    void setCodeRangeableWrapper(DynamicObject object, StringCodeRangeableWrapper codeRangeableWrapper);

}
