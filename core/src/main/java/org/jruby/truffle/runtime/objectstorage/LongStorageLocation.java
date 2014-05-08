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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

/**
 * A storage location for longs.
 */
public class LongStorageLocation extends PrimitiveStorageLocation {

    public LongStorageLocation(ObjectLayout objectLayout, long offset, int mask) {
        super(objectLayout, offset, mask);
    }

    @Override
    public Object read(ObjectStorage object, boolean condition) {
        try {
            return readLong(object, condition);
        } catch (UnexpectedResultException e) {
            return e.getResult();
        }
    }

    public long readLong(ObjectStorage object, boolean condition) throws UnexpectedResultException {
        if (isSet(object)) {
            // TODO(CS): unsafeGetInt has a problem under compilation - pass condition as false and location as null for now
            return CompilerDirectives.unsafeGetInt(object, offset, false /*condition*/, null /*this*/);
        } else {
            throw new UnexpectedResultException(null);
        }
    }

    @Override
    public void write(ObjectStorage object, Object value) throws GeneralizeStorageLocationException {
        if (value instanceof Long) {
            writeLong(object, (long) value);
        } else if (value == null) {
            markAsUnset(object);
        } else {
            throw new GeneralizeStorageLocationException();
        }
    }

    public void writeLong(ObjectStorage object, long value) {
        CompilerDirectives.unsafePutLong(object, offset, value, null);
        markAsSet(object);
    }

    @Override
    public Class getStoredClass() {
        return Long.class;
    }

}
