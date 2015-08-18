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
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.backtrace.Backtrace;

@Layout
public interface ExceptionLayout extends BasicObjectLayout {

    DynamicObjectFactory createExceptionShape(DynamicObject logicalClass,
                                              DynamicObject metaClass);

    DynamicObject createException(DynamicObjectFactory factory,
                                  @Nullable Object message,
                                  @Nullable Backtrace backtrace);

    boolean isException(DynamicObject object);

    Object getMessage(DynamicObject object);
    void setMessage(DynamicObject object, Object message);

    Backtrace getBacktrace(DynamicObject object);
    void setBacktrace(DynamicObject object, Backtrace backtrace);

}
