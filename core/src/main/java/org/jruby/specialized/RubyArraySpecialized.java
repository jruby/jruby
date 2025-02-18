package org.jruby.specialized;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is the base class for all specialized RubyArray.
 *<p></p>
 * Specialized RubyArray use fields rather than an IRubyObject[] to hold their values. When they need
 * to grow or shrink, they unpack those values to a proper IRubyObject[] and fall back on RubyArray
 * logic.
 *<p></p>
 * Subclasses should override all methods that would access the array directly to use the fields,
 * with guards for the packed flag and access outside packed range. This includes the following
 * methods (at the time of this writing...this list will evolve):
 *<p></p>
 * RubyArray{@link #eltInternal(int)}
 * RubyArray{@link #eltInternalSet(int index, IRubyObject value)}
 * RubyArraySpecialized{@link #finishUnpack(IRubyObject nil)}
 * RubyArray{@link #aryDup()}
 * RubyArray{@link #rb_clear(org.jruby.runtime.ThreadContext)}
 * RubyArray{@link #collect(org.jruby.runtime.ThreadContext, org.jruby.runtime.Block)}
 * RubyArray{@link #copyInto(org.jruby.runtime.ThreadContext,IRubyObject[], int)}
 * RubyArray{@link #copyInto(org.jruby.runtime.ThreadContext,IRubyObject[], int, int)}
 * RubyArray{@link #dupImpl(Ruby, RubyClass)}
 * RubyArray{@link #includes(org.jruby.runtime.ThreadContext, IRubyObject)}
 * RubyArray{@link #indexOf(Object)}
 * RubyArray{@link #inspectAry(org.jruby.runtime.ThreadContext)}
 * RubyArray{@link #internalRotate(org.jruby.runtime.ThreadContext, int)}
 * RubyArray{@link #internalRotateBang(org.jruby.runtime.ThreadContext, int)}
 * RubyArray{@link #op_plus(org.jruby.runtime.ThreadContext, IRubyObject)}
 * RubyArray{@link #reverse_bang(org.jruby.runtime.ThreadContext)}
 * RubyArray{@link #safeReverse()}
 * RubyArray{@link #sortInternal(org.jruby.runtime.ThreadContext, org.jruby.runtime.Block)}
 * RubyArray{@link #sortInternal(org.jruby.runtime.ThreadContext, boolean)}
 * RubyArray{@link #storeInternal(org.jruby.runtime.ThreadContext, int, IRubyObject)}
 * RubyArray{@link #subseq(RubyClass, long, long, boolean)}
 * RubyArray{@link #toJavaArray(ThreadContext)}
 * RubyArray{@link #uniq(org.jruby.runtime.ThreadContext)}
 */
public abstract class RubyArraySpecialized extends RubyArray {
    public static final int MAX_PACKED_SIZE = 2;

    public RubyArraySpecialized(Ruby runtime, boolean light) {
        super(runtime, runtime.getArray(), light);
    }

    public RubyArraySpecialized(RubyClass otherClass, boolean light) {
        super(otherClass.getClassRuntime(), otherClass, light);
    }

    public RubyArraySpecialized(RubyClass otherClass) {
        super(otherClass.getClassRuntime(), otherClass);
    }

    protected final void unpack(ThreadContext context) {
        if (!packed()) return;

        // We give some room to grow based on notion that if we grow once we will likely grow more.
        IRubyObject[] values = new IRubyObject[realLength + 2];
        copyInto(context, values, 0);
        this.values = values;
        this.begin = 0;

        finishUnpack(context.nil);
    }

    protected abstract void finishUnpack(IRubyObject nil);

    protected boolean packed() {
        return values == null;
    }
}
