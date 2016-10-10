/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.cext;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;

public final class DataAdapter implements TruffleObject {

    private final DynamicObject object;

    public DataAdapter(DynamicObject object) {
        this.object = object;
    }

    public DynamicObject getObject() {
        return object;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return DataMessageResolutionForeign.ACCESS;
    }
}
