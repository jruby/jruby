/*
 * Adapted from https://github.com/rubinius/rubinius-core-api.
 *
 * Copyright (c) 2011, Evan Phoenix
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the Evan Phoenix nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jruby.truffle.runtime.rubinius;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

public class RubiniusByteArray extends RubyObject {
    private final byte[] bytes;

    public RubiniusByteArray(RubyClass cls, int size) {
        super(cls);
        bytes = new byte[size];
    }

    public RubiniusByteArray(RubyClass cls, byte[] bytes) {
        super(cls);
        this.bytes = bytes;
    }

    public static RubyObject allocate_sized(RubyNode currentNode, RubyClass baClass, long size) {
        if (size < 0) {
            RubiniusLibrary.throwArgumentError(currentNode, "negative byte array size");
        } else if (size > Integer.MAX_VALUE) {
            RubiniusLibrary.throwArgumentError(currentNode, "too large byte array size");
        }
        return new RubiniusByteArray(baClass, (int) size);
    }

    public int size() {
        return bytes.length;
    }

    public RubyObject fetch_bytes(RubyNode currentNode, long src, long cnt) {
        if (src < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "start less than zero");
        } else if (cnt < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "count less than zero");
        } else if ((src + cnt) > size()) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "fetch is more than available bytes");
        }
        RubiniusByteArray ba = new RubiniusByteArray(getLogicalClass(), (int) cnt + 1);
        System.arraycopy(this.bytes, (int) src, ba.bytes, 0, (int) cnt);
        ba.bytes[(int) cnt] = 0;
        return ba;
    }

    public void move_bytes(RubyNode currentNode, long src, long cnt, long dst) {
        if (src < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "start less than zero");
        } else if (dst < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "dest less than zero");
        } else if (cnt < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "count less than zero");
        } else if ((dst + cnt) > size()) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "move is beyond end of bytearray");
        } else if ((src + cnt) > size()) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "move is more than available bytes");
        }
        System.arraycopy(this.bytes, (int) src, this.bytes, (int) dst, (int) cnt);
    }

    public int get_byte(RubyNode currentNode, long idx) {
        if (idx < 0 || idx >= size()) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "index out of bounds");
        }
        return this.bytes[(int) idx];
    }

    public int set_byte(RubyNode currentNode, long idx, long value) {
        if (idx < 0 || idx >= size()) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "index out of bounds");
        }
        this.bytes[(int) idx] = (byte) value;
        return this.bytes[(int) idx];
    }

    public int compare_bytes(RubyNode currentNode, RubiniusByteArray other, long slim, long olim) {
        if (slim < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "bytes of self to compare is less than zero");
        } else if (olim < 0) {
            RubiniusLibrary.throwObjectBoundsExceededError(currentNode, "bytes of other to compare is less than zero");
        }
        long m = size() < slim ? size() : slim;
        long n = other.size() < olim ? other.size() : olim;
        long len = m < n ? m : n;
        long cmp = ByteList.memcmp(this.bytes, 0, other.bytes, 0, (int) len);
        if (cmp == 0) {
            if (m < n) {
                return -1;
            } else if (m > n) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return cmp < 0 ? -1 : 1;
        }
    }

    public Object locate(RubyString pattern, long start, long max) {
        byte[] pat = pattern.getBytes().bytes();
        int len = pat.length;
        if (len == 0) {
            return start;
        }
        if (max == 0 || max > size()) {
            max = size();
        }
        max -= (len - 1);
        for (int i = (int) start; i < max; i++) {
            if (this.bytes[i] == pat[0]) {
                int j;
                for (j = 1; j < len; j++) {
                    if (this.bytes[i + j] != pat[j]) {
                        break;
                    }
                }
                if (j == len) {
                    return i + len;
                }
            }
        }
        return NilPlaceholder.INSTANCE;
    }

    public RubyObject prepend(RubyString str) {
        RubiniusByteArray ba = new RubiniusByteArray(getLogicalClass(), size() + str.getBytes().getRealSize());
        System.arraycopy(str.getBytes().unsafeBytes(), str.getBytes().getBegin(), ba.bytes, 0, str.getBytes().getRealSize());
        System.arraycopy(bytes, 0, ba.bytes, str.getBytes().getRealSize(), size());
        return ba;
    }

    public RubyObject utf8_char(RubyNode currentNode, RubyObject offset) {
        throw new RaiseException(currentNode.getContext().getCoreLibrary().runtimeError("ByteArray#utf8_char not implemented", currentNode));
    }

    public RubyObject reverse(long start, long total) {
        if (total <= 0 || start < 0 || start >= size()) {
            return this;
        }
        int pos1 = (int) start;
        int pos2 = (int) total - 1;
        byte tmp;
        while (pos1 < pos2) {
            tmp = bytes[pos1];
            bytes[pos1++] = bytes[pos2];
            bytes[pos2--] = tmp;
        }
        return this;
    }
}
