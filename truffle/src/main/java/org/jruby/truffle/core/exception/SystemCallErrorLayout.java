/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.exception;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.jruby.truffle.language.backtrace.Backtrace;

@Layout
public interface SystemCallErrorLayout extends ExceptionLayout {

    DynamicObjectFactory createSystemCallErrorShape(
            DynamicObject logicalClass,
            DynamicObject metaClass);

    DynamicObject createSystemCallError(
            DynamicObjectFactory factory,
            Object message,
            @Nullable Backtrace backtrace,
            Object errno);

    Object getErrno(DynamicObject object);
    void setErrno(DynamicObject object, Object value);

}
