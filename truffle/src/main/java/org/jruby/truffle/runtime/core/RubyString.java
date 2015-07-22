/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

@Deprecated
public class RubyString extends RubyBasicObject {

    public ByteList bytes;
    public int codeRange = StringSupport.CR_UNKNOWN;
    public StringCodeRangeableWrapper codeRangeableWrapper;

    public RubyString(RubyBasicObject stringClass, ByteList bytes, DynamicObject dynamicObject) {
        super(stringClass, dynamicObject);
        this.bytes = bytes;
    }

}
