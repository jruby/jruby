/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
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
 * Copyright (C) 2007-2010 JRuby Community
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby.truffle.parser;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringSupport;
import org.jruby.truffle.language.RubyNode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.jruby.truffle.core.rope.CodeRange.CR_UNKNOWN;

public class ParserByteList {

    private static class ParserByteListNode extends RubyNode {

        @Child RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child RopeNodes.WithEncodingNode withEncodingNode;

        public ParserByteListNode() {
            makeSubstringNode = RopeNodes.MakeSubstringNode.create();
            withEncodingNode = RopeNodes.WithEncodingNode.create();
            adoptChildren();
        }

        public RopeNodes.MakeSubstringNode getMakeSubstringNode() {
            return makeSubstringNode;
        }

        public RopeNodes.WithEncodingNode getWithEncodingNode() {
            return withEncodingNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return nil();
        }
    }

    private final Rope rope;
    private ParserByteListNode parserByteListNode;

    public ParserByteList(Rope rope) {
        this.rope = rope;
    }

    public ParserByteList(byte[] bytes) {
        this(bytes, ASCIIEncoding.INSTANCE);
    }

    public ParserByteList(byte[] bytes, Encoding encoding) {
        this(RopeOperations.create(bytes, encoding, CR_UNKNOWN));
    }

    public ParserByteList(byte[] bytes, int start, int length, Encoding encoding) {
        this(RopeOperations.create(Arrays.copyOfRange(bytes, start, start + length), encoding, CR_UNKNOWN));
    }

    public int getLength() {
        return rope.byteLength();
    }

    public Encoding getEncoding() {
        return rope.getEncoding();
    }

    public ParserByteList withEncoding(Encoding encoding) {
        final Rope newRope = getParseByteListNode().getWithEncodingNode().executeWithEncoding(rope, encoding, CR_UNKNOWN);

        if (newRope == rope) {
            return this;
        }

        return new ParserByteList(newRope);
    }

    public ParserByteList makeShared(int sharedStart, int sharedLength) {
        final Rope newRope = getParseByteListNode().getMakeSubstringNode().executeMake(rope, sharedStart, sharedLength);

        if (newRope == rope) {
            return this;
        }

        return new ParserByteList(newRope);
    }

    public int caseInsensitiveCmp(ParserByteList other) {
        return RopeOperations.caseInsensitiveCmp(rope, other.rope);
    }

    public boolean equal(ParserByteList other) {
        return rope.equals(other.rope);
    }

    public int charAt(int index) {
        return rope.get(index);
    }

    @Override
    public String toString() {
        return RopeOperations.decodeRope(StandardCharsets.ISO_8859_1, rope);
    }

    public Rope toRope() {
        return rope;
    }

    public byte[] getBytes() {
        return rope.getBytes();
    }

    public CodeRange codeRangeScan() {
        return rope.getCodeRange();
    }

    public int getStringLength() {
        return rope.characterLength();
    }

    public int getEncodingLength(Encoding encoding) {
        if ((encoding == rope.getEncoding() && rope.isSingleByteOptimizable()) || encoding.isSingleByte()) {
            return 1;
        }

        return StringSupport.encFastMBCLen(rope.getBytes(), 0, rope.byteLength(), encoding);
    }

    public String toEncodedString() {
        return RopeOperations.decodeRope(rope);
    }

    private ParserByteListNode getParseByteListNode() {
        if (parserByteListNode == null) {
            parserByteListNode = new ParserByteListNode();
        }

        return parserByteListNode;
    }

}
