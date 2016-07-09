/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.regexp;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.joni.Region;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface MatchDataLayout extends BasicObjectLayout {

    DynamicObjectFactory createMatchDataShape(DynamicObject logicalClass,
                                              DynamicObject metaClass);

    DynamicObject createMatchData(DynamicObjectFactory factory,
                                  DynamicObject source,
                                  DynamicObject regexp,
                                  Region region,
                                  Object[] values,
                                  DynamicObject pre,
                                  DynamicObject post,
                                  DynamicObject global,
                                  @Nullable Region charOffsets);

    boolean isMatchData(DynamicObject object);
    boolean isMatchData(Object object);

    DynamicObject getSource(DynamicObject object);
    DynamicObject getRegexp(DynamicObject object);
    Region getRegion(DynamicObject object);
    Object[] getValues(DynamicObject object);
    DynamicObject getPre(DynamicObject object);
    DynamicObject getPost(DynamicObject object);
    DynamicObject getGlobal(DynamicObject object);

    Region getCharOffsets(DynamicObject object);
    void setCharOffsets(DynamicObject object, Region value);

}
