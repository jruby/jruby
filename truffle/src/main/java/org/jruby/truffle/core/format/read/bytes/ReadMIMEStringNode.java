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
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.StringSupport;

import java.nio.ByteBuffer;

@NodeChildren({
        @NodeChild(value = "value", type = SourceNode.class),
})
public abstract class ReadMIMEStringNode extends FormatNode {

    public ReadMIMEStringNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        final ByteBuffer encode = ByteBuffer.wrap(source, getSourcePosition(frame), getSourceLength(frame) - getSourcePosition(frame));

        byte[] lElem = new byte[Math.max(encode.remaining(),0)];
        int index = 0;
        for(;;) {
            if (!encode.hasRemaining()) break;
            int c = Pack.safeGet(encode);
            if (c != '=') {
                lElem[index++] = (byte)c;
            } else {
                if (!encode.hasRemaining()) break;
                encode.mark();
                int c1 = Pack.safeGet(encode);
                if (c1 == '\n' || (c1 == '\r' && (c1 = Pack.safeGet(encode)) == '\n')) continue;
                int d1 = Character.digit(c1, 16);
                if (d1 == -1) {
                    encode.reset();
                    break;
                }
                encode.mark();
                if (!encode.hasRemaining()) break;
                int c2 = Pack.safeGet(encode);
                int d2 = Character.digit(c2, 16);
                if (d2 == -1) {
                    encode.reset();
                    break;
                }
                byte value = (byte)(d1 << 4 | d2);
                lElem[index++] = value;
            }
        }

        final ByteList result = new ByteList(lElem, 0, index, ASCIIEncoding.INSTANCE, false);
        setSourcePosition(frame, encode.position());

        return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), StringOperations.ropeFromByteList(result, StringSupport.CR_UNKNOWN));
    }

}
