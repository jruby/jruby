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
import org.jcodings.Encoding;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.util.ByteList;

@Layout
public interface EncodingLayout extends BasicObjectLayout {

    DynamicObjectFactory createEncodingShape(DynamicObject logicalClass,
                                             DynamicObject metaClass);

    DynamicObject createEncoding(DynamicObjectFactory factory,
                                 Encoding encoding,
                                 ByteList name,
                                 boolean dummy);

    boolean isEncoding(DynamicObject object);
    boolean isEncoding(Object object);

    Encoding getEncoding(DynamicObject object);

    ByteList getName(DynamicObject object);

    boolean getDummy(DynamicObject object);

}
