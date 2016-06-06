package org.jruby.specialized;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;

import java.lang.reflect.Array;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.runtime.Helpers.arrayOf;

/**
 * Created by headius on 5/28/16.
 */
public class RubyArrayOneObject extends RubyArraySpecialized {
    private IRubyObject value;

    RubyArrayOneObject(Ruby runtime, IRubyObject value) {
        super(runtime);
        this.value = value;
        this.realLength = 1;
    }

    RubyArrayOneObject(RubyArrayOneObject other) {
        this(other.getMetaClass(), other.value);
    }

    public RubyArrayOneObject(RubyClass otherClass, IRubyObject value) {
        super(otherClass);
        this.value = value;
    }

    @Override
    public final IRubyObject eltInternal(int index) {
        if (!ok()) return super.eltInternal(index);
        else if (index == 1) return value;
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public final IRubyObject eltInternalSet(int index, IRubyObject value) {
        if (!ok()) return super.eltInternalSet(index, value);
        if (index == 1) return this.value = value;
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    protected void fail() {
        if (!ok()) return;
        setFlag(Constants.ARRAY_PACKING_FAILED_F, true);
        values = new IRubyObject[]{value};
        value = null;
        begin = 0;
        realLength = 1;
    }

    @Override
    public RubyArray aryDup() {
        return new RubyArrayOneObject(this);
    }

    @Override
    public IRubyObject collect(ThreadContext context, Block block) {
        if (!ok()) return super.collect(context, block);

        return new RubyArrayOneObject(getRuntime(), block.yield(context, value));
    }

    @Override
    public void copyInto(IRubyObject[] target, int start) {
        if (!ok()) {
            super.copyInto(target, start);
            return;
        }
        target[start] = value;
    }

    @Override
    public IRubyObject dup() {
        if (!ok()) return super.dup();
        return new RubyArrayOneObject(this);
    }

    @Override
    public IRubyObject each(ThreadContext context, Block block) {
        if (!ok()) return super.each(context, block);

        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "each", enumLengthFn());

        block.yield(context, value);

        return this;
    }

    @Override
    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, Block block) {
        if (!ok()) return super.fillCommon(context, beg, len, block);

        modifyCheck();

        // See [ruby-core:17483]
        if (len < 0) return this;

        if (len > Integer.MAX_VALUE - beg) throw context.runtime.newArgumentError("argument too big");

        if (len > 1) {
            fail();
            return super.fillCommon(context, beg, len, block);
        }

        value = block.yield(context, RubyFixnum.zero(context.runtime));

        return this;
    }

    @Override
    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, IRubyObject item) {
        if (!ok()) return super.fillCommon(context, beg, len, item);

        modifyCheck();

        // See [ruby-core:17483]
        if (len < 0) return this;

        if (len > Integer.MAX_VALUE - beg) throw context.runtime.newArgumentError("argument too big");

        if (len > 1) {
            fail();
            return super.fillCommon(context, beg, len, item);
        }

        value = item;

        return this;
    }

    @Override
    public boolean includes(ThreadContext context, IRubyObject item) {
        if (!ok()) return super.includes(context, item);

        if (equalInternal(context, value, item)) return true;

        return false;
    }

    @Override
    public int indexOf(Object element) {
        if (!ok()) return super.indexOf(element);

        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element);

            if (convertedElement.equals(value)) return 0;
        }
        return -1;
    }

    @Override
    protected IRubyObject inspectAry(ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyString str = RubyString.newStringLight(runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);
        EncodingUtils.strBufCat(runtime, str, OPEN_BRACKET);
        boolean tainted = isTaint();

        RubyString s = inspect(context, value);
        if (s.isTaint()) tainted = true;
        else str.setEncoding(s.getEncoding());
        str.cat19(s);

        EncodingUtils.strBufCat(runtime, str, CLOSE_BRACKET);

        if (tainted) str.setTaint(true);

        return str;
    }

    @Override
    protected IRubyObject internalRotate(ThreadContext context, int cnt) {
        if (!ok()) return super.internalRotate(context, cnt);

        return aryDup();
    }

    @Override
    protected IRubyObject internalRotateBang(ThreadContext context, int cnt) {
        if (!ok()) return super.internalRotateBang(context, cnt);

        modifyCheck();

        return context.runtime.getNil();
    }

    @Override
    public IRubyObject op_plus(IRubyObject obj) {
        if (!ok()) return super.op_plus(obj);
        RubyArray y = obj.convertToArray();
        if (y.size() == 1) return new RubyArrayOneObject(this);
        return super.op_plus(y);
    }

    @Override
    public IRubyObject reverse_bang() {
        if (!ok()) return super.reverse_bang();

        return this;
    }

    @Override
    protected RubyArray safeReverse() {
        if (!ok()) return super.safeReverse();

        return new RubyArrayOneObject(this);
    }

    @Override
    protected IRubyObject sortInternal(ThreadContext context, Block block) {
        if (!ok()) return super.sortInternal(context, block);

        return this;
    }

    @Override
    protected IRubyObject sortInternal(final ThreadContext context, boolean honorOverride) {
        if (!ok()) return super.sortInternal(context, honorOverride);

        return this;
    }

    @Override
    public IRubyObject store(long index, IRubyObject value) {
        if (!ok()) return super.store(index, value);

        if (index == 1) {
            eltSetOk(index, value);
            return value;
        }

        fail();
        return super.store(index, value);
    }

    @Override
    public IRubyObject[] toJavaArray() {
        if (!ok()) return super.toJavaArray();

        return arrayOf(value);
    }

    @Override
    public IRubyObject uniq(ThreadContext context) {
        if (!ok()) return super.uniq(context);

        return new RubyArrayOneObject(this);
    }

    @Override
    @Deprecated
    public void ensureCapacity(int minCapacity) {
        if (minCapacity == 1) return;
        fail();
        super.ensureCapacity(minCapacity);
    }
}
