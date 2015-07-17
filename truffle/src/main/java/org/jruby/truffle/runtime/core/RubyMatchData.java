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

    public static class MatchDataFields {
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

        public MatchDataFields(RubyBasicObject source, RubyBasicObject regexp, Region region, Object[] values, RubyBasicObject pre, RubyBasicObject post, RubyBasicObject global, int begin, int end) {
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
    }

    public final MatchDataFields fields;

    public RubyMatchData(RubyClass rubyClass, RubyBasicObject source, RubyBasicObject regexp, Region region, Object[] values, RubyBasicObject pre, RubyBasicObject post, RubyBasicObject global, int begin, int end) {
        super(rubyClass);
        assert RubyGuards.isRubyRegexp(regexp);
        fields = new MatchDataFields(source, regexp, region, values, pre, post, global, begin, end);
    }

    public Object[] getValues() {
        return Arrays.copyOf(((RubyMatchData) this).fields.values, ((RubyMatchData) this).fields.values.length);
    }

    public Object[] getCaptures() {
        // There should always be at least one value because the entire matched string must be in the values array.
        // Thus, there is no risk of an ArrayIndexOutOfBoundsException here.
        return ArrayUtils.extractRange(((RubyMatchData) this).fields.values, 1, ((RubyMatchData) this).fields.values.length);
    }

    public Object begin(int index) {
        final int b = (((RubyMatchData) this).fields.region == null) ? ((RubyMatchData) this).fields.begin : ((RubyMatchData) this).fields.region.beg[index];

        if (b < 0) {
            return getContext().getCoreLibrary().getNilObject();
        }

        updateCharOffset();

        return ((RubyMatchData) this).fields.charOffsets.beg[index];
    }

    public Object end(int index) {
        int e = (((RubyMatchData) this).fields.region == null) ? ((RubyMatchData) this).fields.end : ((RubyMatchData) this).fields.region.end[index];

        if (e < 0) {
            return getContext().getCoreLibrary().getNilObject();
        }

        final CodeRangeable sourceWrapped = StringNodes.getCodeRangeable(((RubyMatchData) this).fields.source);
        if (!StringSupport.isSingleByteOptimizable(sourceWrapped, sourceWrapped.getByteList().getEncoding())) {
            updateCharOffset();
            e = ((RubyMatchData) this).fields.charOffsets.end[index];
        }

        return e;
    }

    public int getNumberOfRegions() {
        return ((RubyMatchData) this).fields.region.numRegs;
    }

    public int getBackrefNumber(ByteList value) {
        return RegexpNodes.getRegex(((RubyMatchData) this).fields.regexp).nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), ((RubyMatchData) this).fields.region);
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Object object : ((RubyMatchData) this).fields.values) {
            if (object instanceof RubyBasicObject) {
                ((RubyBasicObject) object).visitObjectGraph(visitor);
            }
        }
    }

    public RubyBasicObject getPre() {
        return ((RubyMatchData) this).fields.pre;
    }

    public RubyBasicObject getPost() {
        return ((RubyMatchData) this).fields.post;
    }

    public RubyBasicObject getGlobal() {
        return ((RubyMatchData) this).fields.global;
    }

    public Region getRegion() {
        return ((RubyMatchData) this).fields.region;
    }

    public RubyBasicObject getSource() {
        return ((RubyMatchData) this).fields.source;
    }

    public RubyBasicObject getRegexp() { return ((RubyMatchData) this).fields.regexp; }

    public Object getFullTuple() {
        return ((RubyMatchData) this).fields.fullTuple;
    }

    public void setFullTuple(Object fullTuple) {
        ((RubyMatchData) this).fields.fullTuple = fullTuple;
    }

    public int getFullBegin() {
        return ((RubyMatchData) this).fields.begin;
    }

    public int getFullEnd() {
        return ((RubyMatchData) this).fields.end;
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
        if (((RubyMatchData) this).fields.charOffsetUpdated) return;

        if (((RubyMatchData) this).fields.charOffsets == null || ((RubyMatchData) this).fields.charOffsets.numRegs < 1)
            ((RubyMatchData) this).fields.charOffsets = new Region(1);

        if (encoding.maxLength() == 1) {
            ((RubyMatchData) this).fields.charOffsets.beg[0] = ((RubyMatchData) this).fields.begin;
            ((RubyMatchData) this).fields.charOffsets.end[0] = ((RubyMatchData) this).fields.end;
            ((RubyMatchData) this).fields.charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[2];
        if (((RubyMatchData) this).fields.begin >= 0) {
            pairs[0] = new Pair();
            pairs[0].bytePos = ((RubyMatchData) this).fields.begin;
            pairs[1] = new Pair();
            pairs[1].bytePos = ((RubyMatchData) this).fields.end;
        }

        updatePairs(value, encoding, pairs);

        if (((RubyMatchData) this).fields.begin < 0) {
            ((RubyMatchData) this).fields.charOffsets.beg[0] = ((RubyMatchData) this).fields.charOffsets.end[0] = -1;
            return;
        }
        Pair key = new Pair();
        key.bytePos = ((RubyMatchData) this).fields.begin;
        ((RubyMatchData) this).fields.charOffsets.beg[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        key.bytePos = ((RubyMatchData) this).fields.end;
        ((RubyMatchData) this).fields.charOffsets.end[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;

        ((RubyMatchData) this).fields.charOffsetUpdated = true;
    }

    private void updateCharOffsetManyRegs(ByteList value, Encoding encoding) {
        if (((RubyMatchData) this).fields.charOffsetUpdated) return;

        final Region regs = ((RubyMatchData) this).fields.region;
        int numRegs = regs.numRegs;

        if (((RubyMatchData) this).fields.charOffsets == null || ((RubyMatchData) this).fields.charOffsets.numRegs < numRegs)
            ((RubyMatchData) this).fields.charOffsets = new Region(numRegs);

        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                ((RubyMatchData) this).fields.charOffsets.beg[i] = regs.beg[i];
                ((RubyMatchData) this).fields.charOffsets.end[i] = regs.end[i];
            }
            ((RubyMatchData) this).fields.charOffsetUpdated = true;
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
                ((RubyMatchData) this).fields.charOffsets.beg[i] = ((RubyMatchData) this).fields.charOffsets.end[i] = -1;
                continue;
            }
            key.bytePos = regs.beg[i];
            ((RubyMatchData) this).fields.charOffsets.beg[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
            key.bytePos = regs.end[i];
            ((RubyMatchData) this).fields.charOffsets.end[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        }

        ((RubyMatchData) this).fields.charOffsetUpdated = true;
    }

    private void updateCharOffset() {
        if (((RubyMatchData) this).fields.charOffsetUpdated) return;

        ByteList value = StringNodes.getByteList(((RubyMatchData) this).fields.source);
        Encoding enc = value.getEncoding();

        if (((RubyMatchData) this).fields.region == null) {
            updateCharOffsetOnlyOneReg(value, enc);
        } else {
            updateCharOffsetManyRegs(value, enc);
        }

        ((RubyMatchData) this).fields.charOffsetUpdated = true;
    }
}
