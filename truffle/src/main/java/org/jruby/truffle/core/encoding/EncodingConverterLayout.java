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
import org.jcodings.transcode.EConv;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface EncodingConverterLayout extends BasicObjectLayout {

    DynamicObjectFactory createEncodingConverterShape(DynamicObject logicalClass,
                                                      DynamicObject metaClass);

    DynamicObject createEncodingConverter(DynamicObjectFactory factory,
                                          @Nullable EConv econv);

    boolean isEncodingConverter(DynamicObject object);

    EConv getEconv(DynamicObject object);
    void setEconv(DynamicObject object, EConv value);

}
