/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

public abstract class ArrayCoreMethodNode extends CoreMethodNode {

    public ArrayCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public ArrayCoreMethodNode(ArrayCoreMethodNode prev) {
        super(prev);
    }

    protected boolean isNull(RubyArray array) {
        return array.store == null;
    }

    protected boolean isIntegerFixnum(RubyArray array) {
        return array.store instanceof int[];
    }

    protected boolean isLongFixnum(RubyArray array) {
        return array.store instanceof long[];
    }

    protected boolean isFloat(RubyArray array) {
        return array.store instanceof double[];
    }

    protected boolean isObject(RubyArray array) {
        return array.store instanceof Object[];
    }

    protected boolean isOtherNull(RubyArray array, RubyArray other) {
        return other.store == null;
    }

    protected boolean isOtherIntegerFixnum(RubyArray array, RubyArray other) {
        return other.store instanceof int[];
    }

    protected boolean isOtherLongFixnum(RubyArray array, RubyArray other) {
        return other.store instanceof long[];
    }

    protected boolean isOtherFloat(RubyArray array, RubyArray other) {
        return other.store instanceof double[];
    }

    protected boolean isOtherObject(RubyArray array, RubyArray other) {
        return other.store instanceof Object[];
    }

    protected boolean areBothNull(RubyArray a, RubyArray b) {
        return a.store == null && b.store == null;
    }

    protected boolean areBothIntegerFixnum(RubyArray a, RubyArray b) {
        return a.store instanceof int[] && b.store instanceof int[];
    }

    protected boolean areBothLongFixnum(RubyArray a, RubyArray b) {
        return a.store instanceof long[] && b.store instanceof long[];
    }

    protected boolean areBothFloat(RubyArray a, RubyArray b) {
        return a.store instanceof double[] && b.store instanceof double[];
    }

    protected boolean areBothObject(RubyArray a, RubyArray b) {
        return a.store instanceof Object[] && b.store instanceof Object[];
    }

}
