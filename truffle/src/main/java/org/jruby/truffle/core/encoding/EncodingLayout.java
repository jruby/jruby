/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.jcodings.Encoding;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.jruby.util.ByteList;

@Layout
public interface EncodingLayout extends BasicObjectLayout {

    DynamicObjectFactory createEncodingShape(DynamicObject logicalClass,
                                             DynamicObject metaClass);

    DynamicObject createEncoding(DynamicObjectFactory factory,
                                 @Nullable Encoding encoding,
                                 ByteList name,
                                 boolean dummy);

    boolean isEncoding(DynamicObject object);
    boolean isEncoding(Object object);

    Encoding getEncoding(DynamicObject object);
    void setEncoding(DynamicObject object, Encoding value);

    ByteList getName(DynamicObject object);

    boolean getDummy(DynamicObject object);

}
