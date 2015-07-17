/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transposed from org.jruby.RubyMatchData,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.jruby.truffle.runtime.core;

import org.jcodings.Encoding;
import org.joni.Region;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.RegexpNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

import java.util.Arrays;

/**
 * Represents the Ruby {@code MatchData} class.
 */
public class RubyMatchData extends RubyBasicObject {

    private final RubyBasicObject source;
    private final RubyBasicObject regexp;
    private final Region region;
    private final Object[] values;
    private final RubyBasicObject pre;
    private final RubyBasicObject post;
    private final RubyBasicObject global;
    boolean charOffsetUpdated;
    Region charOffsets;
    private final int begin, end;
    private Object fullTuple;

    public RubyMatchData(RubyClass rubyClass, RubyBasicObject source, RubyBasicObject regexp, Region region, Object[] values, RubyBasicObject pre, RubyBasicObject post, RubyBasicObject global, int begin, int end) {
        super(rubyClass);
        assert RubyGuards.isRubyRegexp(regexp);
        this.source = source;
        this.regexp = regexp;
        this.region = region;
        this.values = values;
        this.pre = pre;
        this.post = post;
        this.global = global;
        this.begin = begin;
        this.end = end;
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
        final int b = (region == null) ? begin : region.beg[index];

        if (b < 0) {
            return getContext().getCoreLibrary().getNilObject();
        }

        updateCharOffset();

        return charOffsets.beg[index];
    }

    public Object end(int index) {
        int e = (region == null) ? end : region.end[index];

        if (e < 0) {
            return getContext().getCoreLibrary().getNilObject();
        }

        final CodeRangeable sourceWrapped = StringNodes.getCodeRangeable(source);
        if (!StringSupport.isSingleByteOptimizable(sourceWrapped, sourceWrapped.getByteList().getEncoding())) {
            updateCharOffset();
            e = charOffsets.end[index];
        }

        return e;
    }

    public int getNumberOfRegions() {
        return region.numRegs;
    }

    public int getBackrefNumber(ByteList value) {
        return RegexpNodes.getRegex(regexp).nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), region);
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Object object : values) {
            if (object instanceof RubyBasicObject) {
                ((RubyBasicObject) object).visitObjectGraph(visitor);
            }
        }
    }

    public RubyBasicObject getPre() {
        return pre;
    }

    public RubyBasicObject getPost() {
        return post;
    }

    public RubyBasicObject getGlobal() {
        return global;
    }

    public Region getRegion() {
        return region;
    }

    public RubyBasicObject getSource() {
        return source;
    }

    public RubyBasicObject getRegexp() { return regexp; }

    public Object getFullTuple() {
        return fullTuple;
    }

    public void setFullTuple(Object fullTuple) {
        this.fullTuple = fullTuple;
    }

    public int getFullBegin() {
        return begin;
    }

    public int getFullEnd() {
        return end;
    }

    // Taken from org.jruby.RubyMatchData.

    private static final class Pair implements Comparable<Pair> {
        int bytePos, charPos;

        @Override
        public int compareTo(Pair pair) {
            return bytePos - pair.bytePos;
        }
    }

    private void updatePairs(ByteList value, Encoding encoding, Pair[] pairs) {
        Arrays.sort(pairs);

        int length = pairs.length;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int s = p;
        int c = 0;

        for (int i = 0; i < length; i++) {
            int q = s + pairs[i].bytePos;
            c += StringSupport.strLength(encoding, bytes, p, q);
            pairs[i].charPos = c;
            p = q;
        }
    }


    private void updateCharOffsetOnlyOneReg(ByteList value, Encoding encoding) {
        if (charOffsetUpdated) return;

        if (charOffsets == null || charOffsets.numRegs < 1) charOffsets = new Region(1);

        if (encoding.maxLength() == 1) {
            charOffsets.beg[0] = begin;
            charOffsets.end[0] = end;
            charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[2];
        if (begin >= 0) {
            pairs[0] = new Pair();
            pairs[0].bytePos = begin;
            pairs[1] = new Pair();
            pairs[1].bytePos = end;
        }

        updatePairs(value, encoding, pairs);

        if (begin < 0) {
            charOffsets.beg[0] = charOffsets.end[0] = -1;
            return;
        }
        Pair key = new Pair();
        key.bytePos = begin;
        charOffsets.beg[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        key.bytePos = end;
        charOffsets.end[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;

        charOffsetUpdated = true;
    }

    private void updateCharOffsetManyRegs(ByteList value, Encoding encoding) {
        if (charOffsetUpdated) return;

        final Region regs = this.region;
        int numRegs = regs.numRegs;

        if (charOffsets == null || charOffsets.numRegs < numRegs) charOffsets = new Region(numRegs);

        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                charOffsets.beg[i] = regs.beg[i];
                charOffsets.end[i] = regs.end[i];
            }
            charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[numRegs * 2];
        for (int i = 0; i < pairs.length; i++) pairs[i] = new Pair();

        int numPos = 0;
        for (int i = 0; i < numRegs; i++) {
            if (regs.beg[i] < 0) continue;
            pairs[numPos++].bytePos = regs.beg[i];
            pairs[numPos++].bytePos = regs.end[i];
        }

        updatePairs(value, encoding, pairs);

        Pair key = new Pair();
        for (int i = 0; i < regs.numRegs; i++) {
            if (regs.beg[i] < 0) {
                charOffsets.beg[i] = charOffsets.end[i] = -1;
                continue;
            }
            key.bytePos = regs.beg[i];
            charOffsets.beg[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
            key.bytePos = regs.end[i];
            charOffsets.end[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        }

        charOffsetUpdated = true;
    }

    private void updateCharOffset() {
        if (charOffsetUpdated) return;

        ByteList value = StringNodes.getByteList(source);
        Encoding enc = value.getEncoding();

        if (region == null) {
            updateCharOffsetOnlyOneReg(value, enc);
        } else {
            updateCharOffsetManyRegs(value, enc);
        }

        charOffsetUpdated = true;
    }
}
