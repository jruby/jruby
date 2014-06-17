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
import com.oracle.truffle.api.source.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.util.cli.Options;

public abstract class ArrayCoreMethodNode extends CoreMethodNode {

    public ArrayCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public ArrayCoreMethodNode(ArrayCoreMethodNode prev) {
        super(prev);
    }

    protected boolean isNull(RubyArray array) {
        return array.getStore() == null;
    }

    protected boolean isIntegerFixnum(RubyArray array) {
        return array.getStore() instanceof int[];
    }

    protected boolean isLongFixnum(RubyArray array) {
        return array.getStore() instanceof long[];
    }

    protected boolean isFloat(RubyArray array) {
        return array.getStore() instanceof double[];
    }

    protected boolean isObject(RubyArray array) {
        return array.getStore() instanceof Object[];
    }

    protected boolean isOtherNull(RubyArray array, RubyArray other) {
        return other.getStore() == null;
    }

    protected boolean isOtherIntegerFixnum(RubyArray array, RubyArray other) {
        return other.getStore() instanceof int[];
    }

    protected boolean isOtherLongFixnum(RubyArray array, RubyArray other) {
        return other.getStore() instanceof long[];
    }

    protected boolean isOtherFloat(RubyArray array, RubyArray other) {
        return other.getStore() instanceof double[];
    }

    protected boolean isOtherObject(RubyArray array, RubyArray other) {
        return other.getStore() instanceof Object[];
    }

    protected boolean areBothNull(RubyArray a, RubyArray b) {
        return a.getStore() == null && b.getStore() == null;
    }

    protected boolean areBothIntegerFixnum(RubyArray a, RubyArray b) {
        return a.getStore() instanceof int[] && b.getStore() instanceof int[];
    }

    protected boolean areBothLongFixnum(RubyArray a, RubyArray b) {
        return a.getStore() instanceof long[] && b.getStore() instanceof long[];
    }

    protected boolean areBothFloat(RubyArray a, RubyArray b) {
        return a.getStore() instanceof double[] && b.getStore() instanceof double[];
    }

    protected boolean areBothObject(RubyArray a, RubyArray b) {
        return a.getStore() instanceof Object[] && b.getStore() instanceof Object[];
    }

    protected boolean areIntArraysEnabled() {
        return Options.TRUFFLE_ARRAYS_INT.load();
    }

}
