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
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
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

import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraysUtil;

public class RubiniusTuple extends RubyObject {

    private IRubyObject[] ary;

    public RubiniusTuple(Ruby runtime, int size) {
        super(runtime, (RubyClass) runtime.getClassFromPath("Rubinius::Tuple"));

        this.ary = new IRubyObject[size];
        RuntimeHelpers.fillNil(ary, runtime);
    }

    public RubiniusTuple(Ruby runtime, int size, IRubyObject fill) {
        super(runtime, (RubyClass) runtime.getClassFromPath("Rubinius::Tuple"));

        this.ary = new IRubyObject[size];
        if (fill == runtime.getNil()) {
            RuntimeHelpers.fillNil(ary, runtime);
        } else {
            Arrays.fill(this.ary, fill);
        }
    }

    public static void createTupleClass(Ruby runtime) {
        RubyClass tupleClass = runtime.getOrCreateModule("Rubinius").defineClassUnder("Tuple", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        tupleClass.setReifiedClass(RubiniusTuple.class);

        tupleClass.defineAnnotatedMethods(RubiniusTuple.class);
    }

    public int num_fields() {
        return ary.length;
    }

    public IRubyObject field(int index) {
        return ary[index];
    }

    private void bounds_exceeded_error(ThreadContext state, String method, long index) {
        throw RubiniusLibrary.object_bounds_exceeded_error(state, method + ": index " + index + " out of bounds for size " + ary.length);
    }

    static RubiniusTuple create(ThreadContext context, int size) {
        return new RubiniusTuple(context.runtime, size);
    }

    static RubiniusTuple create(ThreadContext context, int size, IRubyObject fill) {
        return new RubiniusTuple(context.runtime, size, fill);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject allocate(ThreadContext state, IRubyObject baClass, IRubyObject size) {
        throw state.runtime.newTypeError("Tuple cannot be created via allocate()");
    }

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject rbNew(ThreadContext state, IRubyObject tupleCls, IRubyObject fields) {
        long size = fields.convertToInteger().getLongValue();
        if (size < 0) {
            RubiniusLibrary.argument_error(state, "negative tuple size");
        } else if (size > Integer.MAX_VALUE) {
            RubiniusLibrary.argument_error(state, "too large tuple size");
        }
        return RubiniusTuple.create(state, (int) size);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject pattern(ThreadContext state, IRubyObject size, IRubyObject obj) {
        long cnt = size.convertToInteger().getLongValue();

        if (cnt < 0) {
            RubiniusLibrary.argument_error(state, "negative tuple size");
        } else if (cnt > Integer.MAX_VALUE) {
            RubiniusLibrary.argument_error(state, "too large tuple size");
        }

        return RubiniusTuple.create(state, (int) cnt, obj);
    }

    @JRubyMethod(name = {"[]", "at"})
    public IRubyObject op_aref(ThreadContext state, IRubyObject index) {
        long idx = index.convertToInteger().getLongValue();

        if (idx < 0 || num_fields() <= idx) {
            bounds_exceeded_error(state, "Tuple::put_prim", idx);
        }

        return ary[(int) idx];
    }

    @JRubyMethod(name = {"[]=", "put"})
    public IRubyObject op_aset(ThreadContext state, IRubyObject index, IRubyObject val) {
        long idx = index.convertToInteger().getLongValue();

        if (idx < 0 || num_fields() <= idx) {
            bounds_exceeded_error(state, "Tuple::put_prim", idx);
        }

        this.ary[(int) idx] = val;
        return val;
    }

    @JRubyMethod
    public IRubyObject fields(ThreadContext state) {
        return RubyFixnum.newFixnum(state.runtime, ary.length);
    }

    @JRubyMethod(required = 4)
    public IRubyObject copy_from(ThreadContext state, IRubyObject[] args) {
        IRubyObject _other = args[0];
        IRubyObject start = args[1];
        IRubyObject length = args[2];
        IRubyObject dest = args[3];

        RubiniusTuple other = (RubiniusTuple) _other;
        int osize = other.num_fields();
        int size = this.num_fields();

        long src_start = start.convertToInteger().getLongValue();
        long dst_start = dest.convertToInteger().getLongValue();
        long len = length.convertToInteger().getLongValue();

        // left end should be within range
        if (src_start < 0 || src_start > osize) {
            other.bounds_exceeded_error(state, "Tuple::copy_from", src_start);
        }

        if (dst_start < 0 || dst_start > size) {
            bounds_exceeded_error(state, "Tuple::copy_from", dst_start);
        }

        // length can not be negative and must fit in src/dest
        if (len < 0) {
            other.bounds_exceeded_error(state, "Tuple::copy_from", len);
        }

        if ((src_start + len) > osize) {
            other.bounds_exceeded_error(state, "Tuple::copy_from", src_start + len);
        }

        if (len > (size - dst_start)) {
            bounds_exceeded_error(state, "Tuple::copy_from", len);
        }

        // A memmove within the tuple
        if (other == this) {
            // No movement, no work!
            if (src_start == dst_start) {
                return this;
            }
            // right shift
            if (src_start < dst_start) {
                for (long dest_idx = dst_start + len - 1,
                        src_idx = src_start + len - 1;
                        src_idx >= src_start;
                        src_idx--, dest_idx--) {
                    this.ary[(int) dest_idx] = this.ary[(int) src_idx];
                }
            } else {
                // left shift
                for (long dest_idx = dst_start,
                        src_idx = src_start;
                        src_idx < src_start + len;
                        src_idx++, dest_idx++) {
                    this.ary[(int) dest_idx] = this.ary[(int) src_idx];
                }
            }

        } else {
            for (long src = src_start, dst = dst_start;
                    src < (src_start + len);
                    ++src, ++dst) {
                // Since we have carefully checked the bounds we don't need
                // to do it in at/put
                IRubyObject obj = other.ary[(int) src];
                this.ary[(int) dst] = obj;
            }
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject dup() {
        return super.dup();
    }

    @JRubyMethod
    public IRubyObject delete(ThreadContext state, IRubyObject start, IRubyObject length, IRubyObject obj) {
        int size = this.num_fields();
        long len = length.convertToInteger().getLongValue();
        long lend = start.convertToInteger().getLongValue();
        long rend = lend + len;

        if (size == 0 || len == 0) {
            return RubyFixnum.zero(state.runtime);
        }
        if (lend < 0 || lend >= size) {
            bounds_exceeded_error(state, "Tuple::delete_inplace", lend);
        }

        if (rend < 0 || rend > size) {
            bounds_exceeded_error(state, "Tuple::delete_inplace", rend);
        }

        int i = (int) lend;
        while (i < rend) {
            if (this.ary[i] == obj) {
                int j = i;
                ++i;
                while (i < rend) {
                    IRubyObject val = this.ary[i];
                    if (val != obj) {
                        // no need to set write_barrier since it's already
                        // referenced to this object
                        this.ary[j] = val;
                        ++j;
                    }
                    ++i;
                }
                // cleanup all the bins after
                i = j;
                while (i < rend) {
                    this.ary[i] = state.nil;
                    ++i;
                }
                return RubyFixnum.newFixnum(state.runtime, rend - j);
            }
            ++i;
        }
        return RubyFixnum.zero(state.runtime);
    }

    @JRubyMethod
    public IRubyObject reverse(ThreadContext state, IRubyObject o_start, IRubyObject o_total) {
        long start = o_start.convertToInteger().getLongValue();
        long total = o_total.convertToInteger().getLongValue();

        if (total <= 0 || start < 0 || start >= num_fields()) {
            return this;
        }

        long end = start + total - 1;
        if (end >= num_fields()) {
            end = num_fields() - 1;
        }

        int pos1 = (int) start;
        int pos2 = (int) end;

        IRubyObject tmp;
        while (pos1 < pos2) {
            tmp = ary[pos1];
            ary[pos1++] = ary[pos2];
            ary[pos2--] = tmp;
        }

        return this;
    }
}
