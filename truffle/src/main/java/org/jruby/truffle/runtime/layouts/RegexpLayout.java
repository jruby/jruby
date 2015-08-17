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
import org.joni.Regex;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

@Layout
public interface RegexpLayout extends BasicObjectLayout {

    DynamicObjectFactory createRegexpShape(DynamicObject logicalClass, DynamicObject metaClass);

    DynamicObject createRegexp(DynamicObjectFactory factory, @Nullable Regex regex, @Nullable ByteList source, RegexpOptions options, @Nullable Object cachedNames);

    boolean isRegexp(DynamicObject object);

    Regex getRegex(DynamicObject object);

    void setRegex(DynamicObject object, Regex value);

    ByteList getSource(DynamicObject object);

    void setSource(DynamicObject object, ByteList value);

    RegexpOptions getOptions(DynamicObject object);

    void setOptions(DynamicObject object, RegexpOptions value);

    Object getCachedNames(DynamicObject object);

    void setCachedNames(DynamicObject object, Object value);

}
