/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import jnr.posix.FileStat;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface StatLayout extends BasicObjectLayout {

    DynamicObjectFactory createStatShape(DynamicObject logicalClass,
                                         DynamicObject metaClass);

    DynamicObject createStat(DynamicObjectFactory factory,
                             @Nullable FileStat stat);

    FileStat getStat(DynamicObject object);
    void setStat(DynamicObject object, FileStat value);

}
