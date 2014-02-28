/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.objectstorage;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.nodes.*;

/**
 * A storage location for integers.
 */
public class IntegerStorageLocation extends PrimitiveStorageLocation {

    public IntegerStorageLocation(ObjectLayout objectLayout, long offset, int mask) {
        super(objectLayout, offset, mask);
    }

    @Override
    public Object read(ObjectStorage object, boolean condition) {
        try {
            return readInteger(object, condition);
        } catch (UnexpectedResultException e) {
            return e.getResult();
        }
    }

    public int readInteger(ObjectStorage object, boolean condition) throws UnexpectedResultException {
        if (isSet(object)) {
            return CompilerDirectives.unsafeGetInt(object, offset, condition, this);
        } else {
            throw new UnexpectedResultException(null);
        }
    }

    @Override
    public void write(ObjectStorage object, Object value) throws GeneralizeStorageLocationException {
        if (value instanceof Integer) {
            writeInteger(object, (int) value);
        } else if (value == null) {
            markAsUnset(object);
        } else {
            throw new GeneralizeStorageLocationException();
        }
    }

    public void writeInteger(ObjectStorage object, int value) {
        CompilerDirectives.unsafePutInt(object, offset, value, null);
        markAsSet(object);
    }

    @Override
    public Class getStoredClass() {
        return Integer.class;
    }

}
