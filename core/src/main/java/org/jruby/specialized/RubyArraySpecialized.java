package org.jruby.specialized;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is the base class for all specialized RubyArray.
 *
 * Specialized RubyArray use fields rather than an IRubyObject[] to hold their values. When they need
 * to grow or shrink, they unpack those values to a proper IRubyObject[] and fall back on RubyArray
 * logic.
 *
 * Subclasses should override all methods that would access the array directly to use the fields,
 * with guards for the packed flag and access outside packed range. This includes the following
 * methods (at the time of this writing...this list will evolve):
 *
 * RubyArray{@link #eltInternal(int)}
 * RubyArray{@link #eltInternalSet(int index, IRubyObject value)}
 * RubyArraySpecialized{@link #finishUnpack(IRubyObject nil)}
 * RubyArray{@link #aryDup()}
 * RubyArray{@link #rb_clear()}
 * RubyArray{@link #collect(org.jruby.runtime.ThreadContext, org.jruby.runtime.Block)}
 * RubyArray{@link #copyInto(IRubyObject[], int)}
 * RubyArray{@link #copyInto(IRubyObject[], int, int)}
 * RubyArray{@link #dupImpl()}
 * RubyArray{@link #includes(org.jruby.runtime.ThreadContext, IRubyObject)}
 * RubyArray{@link #indexOf(Object)}
 * RubyArray{@link #inspectAry(org.jruby.runtime.ThreadContext)}
 * RubyArray{@link #internalRotate(org.jruby.runtime.ThreadContext, int)}
 * RubyArray{@link #internalRotateBang(org.jruby.runtime.ThreadContext, int)}
 * RubyArray{@link #op_plus(IRubyObject)}
 * RubyArray{@link #reverse_bang()}
 * RubyArray{@link #safeReverse()}
 * RubyArray{@link #sortInternal(org.jruby.runtime.ThreadContext, org.jruby.runtime.Block)}
 * RubyArray{@link #sortInternal(org.jruby.runtime.ThreadContext, boolean)}
 * RubyArray{@link #storeInternal(int, IRubyObject)}
 * RubyArray{@link #subseq(RubyClass, long, long, boolean)}
 * RubyArray{@link #toJavaArray()}
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

    protected final void unpack() {
        if (!packed()) return;

        // CON: I believe most of the time we'll unpack because we need to grow, so give a bit of extra room.
        //      For example, <<, unshift, and push will all just add one to front or back.
        Ruby runtime = getRuntime();
        IRubyObject[] values = new IRubyObject[realLength + 2];
        Helpers.fillNil(values, runtime);
        copyInto(values, 1);
        this.values = values;
        this.begin = 1;

        finishUnpack(runtime.getNil());
    }

    protected abstract void finishUnpack(IRubyObject nil);

    protected boolean packed() {
        return values == null;
    }
}
