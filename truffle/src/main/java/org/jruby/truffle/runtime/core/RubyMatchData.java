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

import org.joni.Regex;
import org.joni.Region;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.ByteList;

import java.util.Arrays;

/**
 * Represents the Ruby {@code MatchData} class.
 */
public class RubyMatchData extends RubyBasicObject {

    private final RubyString source;
    private final Regex regex;
    private final Region region;
    private final Object[] values;
    private final RubyString pre;
    private final RubyString post;
    private final RubyString global;

    public RubyMatchData(RubyClass rubyClass, RubyString source, Regex regex, Region region, Object[] values, RubyString pre, RubyString post, RubyString global) {
        super(rubyClass);
        this.source = source;
        this.regex = regex;
        this.region = region;
        this.values = values;
        this.pre = pre;
        this.post = post;
        this.global = global;
    }

    public Object[] valuesAt(int... indices) {
        RubyNode.notDesignedForCompilation();

        final Object[] result = new Object[indices.length];

        for (int n = 0; n < indices.length; n++) {
            result[n] = values[indices[n]];
        }

        return result;
    }

    public Object[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public Object[] getCaptures() {
        // There should always be at least one value because the entire matched string must be in the values array.
        // Thus, there is no risk of an ArrayIndexOutOfBoundsException here.
        return ArrayUtils.extractRange(values, 1, values.length);
    }

    public Object begin(int index) {
        if (region == null) {
            throw new UnsupportedOperationException("begin is not yet working when no grouping data is available");
        }

        int begin = region.beg[index];

        if (begin < 0) {
            return getContext().getCoreLibrary().getNilObject();
        }

        return begin;
    }

    public Object end(int index) {
        if (region == null) {
            throw new UnsupportedOperationException("end is not yet working when no grouping data is available");
        }

        int end = region.end[index];

        if (end < 0) {
            return getContext().getCoreLibrary().getNilObject();
        }

        return end;
    }

    public int getNumberOfRegions() {
        return region.numRegs;
    }

    public int getBackrefNumber(ByteList value) {
        return regex.nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), region);
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Object object : values) {
            if (object instanceof RubyBasicObject) {
                ((RubyBasicObject) object).visitObjectGraph(visitor);
            }
        }
    }

    public RubyString getPre() {
        return pre;
    }

    public RubyString getPost() {
        return post;
    }

    public RubyString getGlobal() {
        return global;
    }

    public Region getRegion() {
        return region;
    }

    public RubyString getSource() {
        return source;
    }

}
