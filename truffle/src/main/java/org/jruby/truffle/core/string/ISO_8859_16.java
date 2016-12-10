/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2013 The JRuby Community (jruby.org)
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
 ***** END LICENSE BLOCK *****/

package org.jruby.truffle.core.string;

import sun.nio.cs.US_ASCII;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class ISO_8859_16 extends Charset {
    public static final ISO_8859_16 INSTANCE = new ISO_8859_16();

    ISO_8859_16() {
        super("ISO-8859-16", new String[]{"iso-ir-226", "ISO_8859-16:2001", "ISO_8859-16", "latin10", "l10", "csISO885916", "ISO8859_16", "ISO_8859_16", "8859_16", "ISO8859-16"});
    }
    @Override
    public boolean contains(Charset cs) {
        return cs instanceof US_ASCII || cs instanceof ISO_8859_16;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Decoder extends CharsetDecoder {
        Decoder(Charset charset) {
            super(charset, 1.0f, 1.0f);
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            for (;;) {
                if (!in.hasRemaining()) return CoderResult.UNDERFLOW;
                if (!out.hasRemaining()) return CoderResult.OVERFLOW;
                int b = in.get() & 0xFF;
                char c = TABLE[b];
                out.put(c);
            }
        }

        private static final char[] TABLE;

        static {
            TABLE = new char[256];
            for (int i = 0; i < 256; i++) {
                TABLE[i] = (char)i;
            }
            TABLE[0xA1] = '\u0104';
            TABLE[0xA2] = '\u0105';
            TABLE[0xA3] = '\u0141';
            TABLE[0xA4] = '\u20AC';
            TABLE[0xA5] = '\u201E';
            TABLE[0xA6] = '\u0160';
            TABLE[0xA8] = '\u0161';
            TABLE[0xAA] = '\u0218';
            TABLE[0xAC] = '\u0179';
            TABLE[0xAE] = '\u017A';
            TABLE[0xAF] = '\u017B';

            TABLE[0xB2] = '\u010C';
            TABLE[0xB3] = '\u0142';
            TABLE[0xB4] = '\u017D';
            TABLE[0xB5] = '\u201D';
            TABLE[0xB8] = '\u017E';
            TABLE[0xB9] = '\u010D';
            TABLE[0xBA] = '\u0219';
            TABLE[0xBC] = '\u0152';
            TABLE[0xBD] = '\u0153';
            TABLE[0xBE] = '\u0178';
            TABLE[0xBF] = '\u017C';

            TABLE[0xC3] = '\u0102';
            TABLE[0xC5] = '\u0106';

            TABLE[0xD1] = '\u0110';
            TABLE[0xD2] = '\u0143';
            TABLE[0xD5] = '\u0150';
            TABLE[0xD7] = '\u015A';
            TABLE[0xD8] = '\u0170';
            TABLE[0xDD] = '\u0118';
            TABLE[0xDE] = '\u021A';

            TABLE[0xE3] = '\u0103';
            TABLE[0xE5] = '\u0107';
        }
    }

    private static class Encoder extends CharsetEncoder {
        Encoder(Charset charset) {
            super(charset, 1.0f, 1.0f, new byte[]{(byte)'?'});
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            for (;;) {
                if (!in.hasRemaining()) return CoderResult.UNDERFLOW;
                if (!out.hasRemaining()) return CoderResult.OVERFLOW;
                char c = in.get();
                byte b = 0;
                byte[] replace = null;
                switch (c) {
                    case '\u0104': b = (byte)0xA1; break;
                    case '\u0105': b = (byte)0xA2; break;
                    case '\u0141': b = (byte)0xA3; break;
                    case '\u20AC': b = (byte)0xA4; break;
                    case '\u201E': b = (byte)0xA5; break;
                    case '\u0160': b = (byte)0xA6; break;
                    case '\u0161': b = (byte)0xA8; break;
                    case '\u0218': b = (byte)0xAA; break;
                    case '\u0179': b = (byte)0xAC; break;
                    case '\u017A': b = (byte)0xAE; break;
                    case '\u017B': b = (byte)0xAF; break;

                    case '\u010C': b = (byte)0xB2; break;
                    case '\u0142': b = (byte)0xB3; break;
                    case '\u017D': b = (byte)0xB4; break;
                    case '\u201D': b = (byte)0xB5; break;
                    case '\u017E': b = (byte)0xB8; break;
                    case '\u010D': b = (byte)0xB9; break;
                    case '\u0219': b = (byte)0xBA; break;
                    case '\u0152': b = (byte)0xBC; break;
                    case '\u0153': b = (byte)0xBD; break;
                    case '\u0178': b = (byte)0xBE; break;
                    case '\u017C': b = (byte)0xBF; break;

                    case '\u0102': b = (byte)0xC3; break;
                    case '\u0106': b = (byte)0xC5; break;

                    case '\u0110': b = (byte)0xD1; break;
                    case '\u0143': b = (byte)0xD2; break;
                    case '\u0150': b = (byte)0xD5; break;
                    case '\u015A': b = (byte)0xD7; break;
                    case '\u0170': b = (byte)0xD8; break;
                    case '\u0118': b = (byte)0xDD; break;
                    case '\u021A': b = (byte)0xDE; break;

                    case '\u0103': b = (byte)0xE3; break;
                    case '\u0107': b = (byte)0xE5; break;

                    default:
                        if (c < 256) b = (byte)c;
                        else replace = replacement();
                }

                if (replace != null) {
                    if (out.remaining() < replace.length) {
                        in.position(in.position() - 1);
                        return CoderResult.OVERFLOW;
                    } else {
                        out.put(replace);
                        replace = null;
                    }
                } else {
                    out.put(b);
                }
            }
        }
    }
}
