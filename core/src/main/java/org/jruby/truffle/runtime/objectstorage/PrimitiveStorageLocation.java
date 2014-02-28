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

/**
 * A storage location that uses one of the primitive fields in {@code RubyBasObject}.
 */
public abstract class PrimitiveStorageLocation extends StorageLocation {

    protected final long offset;
    protected final int mask;

    protected PrimitiveStorageLocation(ObjectLayout objectLayout, long offset, int mask) {
        super(objectLayout);
        this.offset = offset;
        this.mask = mask;
    }

    @Override
    public boolean isSet(ObjectStorage object) {
        return (object.primitiveSetMap & mask) != 0;
    }

    protected void markAsSet(ObjectStorage object) {
        object.primitiveSetMap |= mask;
    }

    protected void markAsUnset(ObjectStorage object) {
        object.primitiveSetMap &= ~mask;
    }

}
