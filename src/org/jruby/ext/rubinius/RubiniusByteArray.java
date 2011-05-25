/*
 **** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2011 Charles O Nutter <headius@headius.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.rubinius;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class RubiniusByteArray extends RubyObject {

    private final byte[] bytes;

    RubiniusByteArray(Ruby runtime, int size) {
        // FIXME: need to store metaclass somewhere...
        super(runtime, (RubyClass) runtime.getClassFromPath("Rubinius::ByteArray"));

        bytes = new byte[size];
    }

    RubiniusByteArray(Ruby runtime, byte[] bytes) {
        // FIXME: need to store metaclass somewhere...
        super(runtime, (RubyClass) runtime.getClassFromPath("Rubinius::ByteArray"));

        this.bytes = bytes;
    }

    public static void createByteArrayClass(Ruby runtime) {
        RubyClass baClass = runtime.getOrCreateModule("Rubinius").defineClassUnder("ByteArray", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        baClass.setReifiedClass(RubiniusByteArray.class);

        baClass.defineAnnotatedMethods(RubiniusByteArray.class);
        
        // add "data" method to String for spec purposes; not recommended for use (raw byte[] access)
        runtime.getString().defineAnnotatedMethods(RubiniusString.class);
    }

    static RubiniusByteArray create(ThreadContext context, int size) {
        return new RubiniusByteArray(context.runtime, size);
    }

    private int size() {
        return bytes.length;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject allocate(ThreadContext state, IRubyObject baClass, IRubyObject size) {
        throw state.runtime.newTypeError("ByteArray cannot be created via allocate()");
    }

    @JRubyMethod(name = {"new", "allocate_sized"}, meta = true)
    public static IRubyObject allocate_sized(ThreadContext state, IRubyObject baClass, IRubyObject bytes) {
        long size = bytes.convertToInteger().getLongValue();
        if(size < 0) {
          RubiniusLibrary.argument_error(state, "negative byte array size");
        } else if (size > Integer.MAX_VALUE) {
          RubiniusLibrary.argument_error(state, "too large byte array size");
        }
        return RubiniusByteArray.create(state, (int)size);
    }

    @JRubyMethod
    public IRubyObject fetch_bytes(ThreadContext state, IRubyObject start, IRubyObject count) {
        long src = start.convertToInteger().getLongValue();
        long cnt = count.convertToInteger().getLongValue();

        if (src < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "start less than zero");
        } else if (cnt < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "count less than zero");
        } else if ((src + cnt) > size()) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "fetch is more than available bytes");
        }

        RubiniusByteArray ba = RubiniusByteArray.create(state, (int) cnt + 1);
        System.arraycopy(this.bytes, (int) src, ba.bytes, 0, (int) cnt);
        ba.bytes[(int) cnt] = 0;

        return ba;
    }

    @JRubyMethod
    public IRubyObject move_bytes(ThreadContext state, IRubyObject start, IRubyObject count, IRubyObject dest) {
        long src = start.convertToInteger().getLongValue();
        long cnt = count.convertToInteger().getLongValue();
        long dst = dest.convertToInteger().getLongValue();

        if (src < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "start less than zero");
        } else if (dst < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "dest less than zero");
        } else if (cnt < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "count less than zero");
        } else if ((dst + cnt) > size()) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "move is beyond end of bytearray");
        } else if ((src + cnt) > size()) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "move is more than available bytes");
        }

        System.arraycopy(this.bytes, (int) src, this.bytes, (int) dst, (int) cnt);

        return count;
    }

    @JRubyMethod
    public IRubyObject get_byte(ThreadContext state, IRubyObject index) {
        long idx = index.convertToInteger().getLongValue();

        if (idx < 0 || idx >= size()) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "index out of bounds");
        }

        return RubyFixnum.newFixnum(state.runtime, this.bytes[(int) idx]);
    }

    @JRubyMethod
    public IRubyObject set_byte(ThreadContext state, IRubyObject index, IRubyObject value) {
        long idx = index.convertToInteger().getLongValue();

        if (idx < 0 || idx >= size()) {
            RubiniusLibrary.object_bounds_exceeded_error(state, "index out of bounds");
        }

        this.bytes[(int) idx] = (byte) value.convertToInteger().getLongValue();
        return RubyFixnum.newFixnum(state.runtime, this.bytes[(int) idx]);
    }

    @JRubyMethod
    public IRubyObject compare_bytes(ThreadContext state, IRubyObject _other, IRubyObject a, IRubyObject b) {
        RubiniusByteArray other = (RubiniusByteArray) _other;
        long slim = a.convertToInteger().getLongValue();
        long olim = b.convertToInteger().getLongValue();

        if (slim < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state,
                    "bytes of self to compare is less than zero");
        } else if (olim < 0) {
            RubiniusLibrary.object_bounds_exceeded_error(state,
                    "bytes of other to compare is less than zero");
        }

        // clamp limits to actual sizes
        long m = size() < slim ? size() : slim;
        long n = other.size() < olim ? other.size() : olim;

        // only compare the shortest string
        long len = m < n ? m : n;

        long cmp = ByteList.memcmp(this.bytes, 0, other.bytes, 0, (int) len);

        // even if substrings are equal, check actual requested limits
        // of comparison e.g. "xyz", "xyzZ"
        if (cmp == 0) {
            if (m < n) {
                return RubyFixnum.minus_one(state.runtime);
            } else if (m > n) {
                return RubyFixnum.one(state.runtime);
            } else {
                return RubyFixnum.zero(state.runtime);
            }
        } else {
            return cmp < 0 ? RubyFixnum.minus_one(state.runtime) : RubyFixnum.one(state.runtime);
        }
    }

    @JRubyMethod
    public IRubyObject size(ThreadContext state) {
        return RubyFixnum.newFixnum(state.runtime, bytes.length);
    }

    @JRubyMethod
    public IRubyObject dup() {
        return super.dup();
    }

    @JRubyMethod
    public IRubyObject locate(ThreadContext state, IRubyObject pattern, IRubyObject start, IRubyObject max_o) {
        byte[] pat = pattern.convertToString().getByteList().bytes();
        int len = pat.length;
        long max = max_o.convertToInteger().getLongValue();

        if (len == 0) {
            return start;
        }

        if (max == 0 || max > size()) {
            max = size();
        }

        max -= (len - 1);

        for (int i = (int) start.convertToInteger().getLongValue(); i < max; i++) {
            if (this.bytes[i] == pat[0]) {
                int j;
                // match the rest of the pattern string
                for (j = 1; j < len; j++) {
                    if (this.bytes[i + j] != pat[j]) {
                        break;
                    }
                }

                // if the full pattern matched, return the index
                // of the end of the pattern in 'this'.
                if (j == len) {
                    return RubyFixnum.newFixnum(state.runtime, i + len);
                }
            }
        }

        return state.nil;
    }

    @JRubyMethod
    public IRubyObject prepend(ThreadContext state, IRubyObject _str) {
        RubyString str = _str.convertToString();
        RubiniusByteArray ba = RubiniusByteArray.create(state, size() + str.size());

        System.arraycopy(str.getByteList().unsafeBytes(), str.getByteList().getBegin(), ba.bytes, 0, str.size());
        System.arraycopy(bytes, 0, ba.bytes, str.size(), size());

        return ba;
    }

    @JRubyMethod
    public IRubyObject utf8_char(ThreadContext state, IRubyObject offset) {
        // not sure what this is supposed to return or best way to get it in JRuby yet
        throw state.runtime.newNotImplementedError("ByteArray#utf8_char not implemented");
    }

    @JRubyMethod
    public IRubyObject reverse(ThreadContext state, IRubyObject o_start, IRubyObject o_total) {
        long start = o_start.convertToInteger().getLongValue();
        long total = o_total.convertToInteger().getLongValue();

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
    
    public static class RubiniusString {
        @JRubyMethod
        public static IRubyObject data(ThreadContext context, IRubyObject string) {
            return new RubiniusByteArray(context.runtime, ((RubyString)string).getByteList().getUnsafeBytes());
        }
    }
}
