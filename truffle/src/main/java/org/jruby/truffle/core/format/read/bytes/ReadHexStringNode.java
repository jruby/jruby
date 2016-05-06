/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some logic copied from jruby.util.Pack
 *
 *  * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby.truffle.core.format.read.bytes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.rope.AsciiOnlyLeafRope;
import org.jruby.util.Pack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@NodeChildren({
        @NodeChild(value = "value", type = SourceNode.class),
})
public abstract class ReadHexStringNode extends FormatNode {

    private final ByteOrder byteOrder;
    private final boolean star;
    private final int length;

    public ReadHexStringNode(RubyContext context, ByteOrder byteOrder, boolean star, int length) {
        super(context);
        this.byteOrder = byteOrder;
        this.star = star;
        this.length = length;
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        final int position = getSourcePosition(frame);

        final ByteBuffer encode = ByteBuffer.wrap(source, position, getSourceLength(frame) - position);

        int occurrences = length;

        if (star || occurrences > encode.remaining() * 2) {
            occurrences = encode.remaining() * 2;
        }

        int bits = 0;

        byte[] lElem = new byte[occurrences];

        final int shift;

        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            shift = 4;
        } else {
            shift = 0;
        }

        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
            if ((lCurByte & 1) != 0) {
                if (byteOrder == ByteOrder.BIG_ENDIAN) {
                    bits <<= 4;
                } else {
                    bits >>>= 4;
                }
            } else {
                bits = encode.get();
            }

            lElem[lCurByte] = Pack.sHexDigits[(bits >>> shift) & 15];
        }

        setSourcePosition(frame, encode.position());

        return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(),
                new AsciiOnlyLeafRope(lElem, USASCIIEncoding.INSTANCE));
    }

}
