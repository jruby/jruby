/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby;

import java.util.Arrays;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class RubyTuple extends RubyObject {
    private IRubyObject[] ary;

    public RubyTuple(Ruby runtime, RubyClass metaclass, int size) {
        super(runtime, metaclass);
        this.ary = new IRubyObject[size];
        RuntimeHelpers.fillNil(ary, runtime);
    }

    public static void createTupleClass(Ruby runtime) {
        RubyClass tupleClass = runtime
                .getOrCreateModule("Rubinius")
                .defineClassUnder("Tuple", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        tupleClass.setReifiedClass(RubyTuple.class);

        tupleClass.defineAnnotatedMethods(RubyTuple.class);
    }

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject tupleCls, IRubyObject cnt) {
        int size = (int)cnt.convertToInteger().getLongValue();
        return new RubyTuple(context.runtime, (RubyClass)tupleCls, size);
    }

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject idx) {
        return ary[(int)((RubyFixnum)idx).getLongValue()];
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject op_aset(ThreadContext context, IRubyObject idx, IRubyObject val) {
        int index = (int)((RubyFixnum)idx).getLongValue();
        if (index >= ary.length) {
            ary = Arrays.copyOf(ary, ary.length * 3 / 2 + 1);
        }
        return ary[index] = val;
    }
}
