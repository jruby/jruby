package org.jruby.specialized;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.runtime.Helpers.arrayOf;

/**
 * One object version of RubyArraySpecialized.
 */
public class RubyArrayOneObject extends RubyArraySpecialized {
    private IRubyObject value;

    public RubyArrayOneObject(Ruby runtime, IRubyObject value) {
        // packed arrays are omitted from ObjectSpace
        super(runtime, false);
        this.value = value;
        this.realLength = 1;
    }

    public RubyArrayOneObject(RubyClass otherClass, IRubyObject value) {
        // packed arrays are omitted from ObjectSpace
        super(otherClass, false);
        this.value = value;
        this.realLength = 1;
    }

    RubyArrayOneObject(RubyArrayOneObject other) {
        this(other.getMetaClass(), other.value);
    }

    RubyArrayOneObject(RubyClass metaClass, RubyArrayOneObject other) {
        this(metaClass, other.value);
    }

    @Override
    public final IRubyObject eltInternal(int index) {
        if (!packed()) return super.eltInternal(index);
        else if (index == 0) return value;
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public final IRubyObject eltInternalSet(int index, IRubyObject value) {
        if (!packed()) return super.eltInternalSet(index, value);
        if (index == 0) return this.value = value;
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    protected void finishUnpack(IRubyObject nil) {
        value = nil;
    }

    @Override
    public RubyArray aryDup() {
        if (!packed()) return super.aryDup();
        return new RubyArrayOneObject(getRuntime().getArray(), this);
    }

    @Override
    public IRubyObject rb_clear() {
        if (!packed()) return super.rb_clear();

        modifyCheck();

        // fail packing, but defer [] creation in case it is never needed
        value = null;
        values = IRubyObject.NULL_ARRAY;
        realLength = 0;

        return this;
    }

    @Override
    public void copyInto(IRubyObject[] target, int start) {
        if (!packed()) {
            super.copyInto(target, start);
            return;
        }
        target[start] = value;
    }

    @Override
    public void copyInto(IRubyObject[] target, int start, int len) {
        if (!packed()) {
            super.copyInto(target, start, len);
            return;
        }
        if (len != 1) {
            unpack();
            super.copyInto(target, start, len);
            return;
        }
        target[start] = value;
    }

    @Override
    protected RubyArray dupImpl(Ruby runtime, RubyClass metaClass) {
        if (!packed()) return super.dupImpl(runtime, metaClass);
        return new RubyArrayOneObject(metaClass, this);
    }

    @Override
    public boolean includes(ThreadContext context, IRubyObject item) {
        if (!packed()) return super.includes(context, item);

        if (equalInternal(context, value, item)) return true;

        return false;
    }

    @Override
    public int indexOf(Object element) {
        if (!packed()) return super.indexOf(element);

        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element);

            if (convertedElement.equals(value)) return 0;
        }
        return -1;
    }

    @Override
    protected IRubyObject inspectAry(ThreadContext context) {
        if (!packed()) return super.inspectAry(context);

        final Ruby runtime = context.runtime;
        RubyString str = RubyString.newStringLight(runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);
        str.cat((byte) '[');
        boolean tainted = isTaint();

        RubyString s = inspect(context, value);
        if (s.isTaint()) tainted = true;
        EncodingUtils.encAssociateIndex(str, s.getEncoding());
        str.cat19(s);

        str.cat((byte) ']');

        if (tainted) str.setTaint(true);

        return str;
    }

    @Override
    protected IRubyObject internalRotate(ThreadContext context, int cnt) {
        if (!packed()) return super.internalRotate(context, cnt);

        return aryDup();
    }

    @Override
    protected IRubyObject internalRotateBang(ThreadContext context, int cnt) {
        if (!packed()) return super.internalRotateBang(context, cnt);

        modifyCheck();

        return context.nil;
    }

    @Override
    public IRubyObject op_plus(IRubyObject obj) {
        if (!packed()) return super.op_plus(obj);
        RubyArray y = obj.convertToArray();
        if (y.size() == 0) return new RubyArrayOneObject(this);
        return super.op_plus(y);
    }

    @Override
    public IRubyObject replace(IRubyObject orig) {
        if (!packed()) return super.replace(orig);

        modifyCheck();

        RubyArray origArr = orig.convertToArray();

        if (this == orig) return this;

        if (origArr.size() == 1) {
            value = origArr.eltInternal(0);
            return this;
        }

        unpack();

        return super.replace(origArr);
    }

    @Override
    public IRubyObject reverse_bang() {
        if (!packed()) return super.reverse_bang();

        return this;
    }

    @Override
    protected RubyArray safeReverse() {
        if (!packed()) return super.safeReverse();

        return new RubyArrayOneObject(this);
    }

    @Override
    protected IRubyObject sortInternal(ThreadContext context, Block block) {
        if (!packed()) return super.sortInternal(context, block);

        return this;
    }

    @Override
    protected IRubyObject sortInternal(final ThreadContext context, boolean honorOverride) {
        if (!packed()) return super.sortInternal(context, honorOverride);

        return this;
    }

    @Override
    protected void storeInternal(final int index, final IRubyObject value) {
        if (index == 0 && packed()) {
            this.value = value;
            return;
        }

        if (packed()) unpack(); // index > 0
        super.storeInternal(index, value);
    }

    @Override
    public IRubyObject subseq(RubyClass metaClass, long beg, long len, boolean light) {
        if (!packed()) return super.subseq(metaClass, beg, len, light);

        if (len == 0) return newEmptyArray(metaClass.getClassRuntime());

        if (beg != 0 || len != 1) {
            unpack();
            return super.subseq(metaClass, beg, len, light);
        }

        return new RubyArrayOneObject(metaClass, this);
    }

    @Override
    public IRubyObject[] toJavaArray() {
        if (!packed()) return super.toJavaArray();

        return arrayOf(value);
    }

    @Override
    public IRubyObject uniq(ThreadContext context) {
        if (!packed()) return super.uniq(context);

        return new RubyArrayOneObject(this);
    }

    @Override
    public RubyArray collectCommon(ThreadContext context, Block block) {
        if (!packed()) return super.collectCommon(context, block);

        if (!block.isGiven()) return makeShared();

        return new RubyArrayOneObject(context.runtime, block.yieldNonArray(context, value, null));
    }

    @Override
    protected RubyArray makeShared() {
        if (!packed()) return super.makeShared();

        return new RubyArrayOneObject(this);
    }
}
