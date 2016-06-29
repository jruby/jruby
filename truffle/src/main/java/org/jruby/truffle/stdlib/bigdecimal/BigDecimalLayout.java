/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

import java.math.BigDecimal;

@Layout
public interface BigDecimalLayout extends BasicObjectLayout {

    DynamicObjectFactory createBigDecimalShape(
            DynamicObject logicalClass,
            DynamicObject metaClass);

    DynamicObject createBigDecimal(
            DynamicObjectFactory factory,
            BigDecimal value,
            BigDecimalType type);

    boolean isBigDecimal(DynamicObject object);

    BigDecimal getValue(DynamicObject object);
    void setValue(DynamicObject object, BigDecimal value);

    BigDecimalType getType(DynamicObject object);
    void setType(DynamicObject object, BigDecimalType value);

}
