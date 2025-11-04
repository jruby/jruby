package org.jruby.specialized;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyArrayNative;
import org.jruby.RubyClass;
import org.jruby.RubyComparable;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.api.Create;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.Helpers.invokedynamic;

/**
 * Two object version of RubyArraySpecialized.
 */
public class RubyArrayTwoObject extends RubyArraySpecialized {
    private IRubyObject car;
    private IRubyObject cdr;

    public RubyArrayTwoObject(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        // packed arrays are omitted from ObjectSpace
        super(runtime, false);
        this.car = car;
        this.cdr = cdr;
        this.realLength = 2;
    }

    public RubyArrayTwoObject(RubyClass otherClass, IRubyObject car, IRubyObject cdr) {
        // packed arrays are omitted from ObjectSpace
        super(otherClass);
        this.car = car;
        this.cdr = cdr;
        this.realLength = 2;
    }

    RubyArrayTwoObject(RubyArrayTwoObject other) {
        this(other.getMetaClass(), other.car, other.cdr);
    }

    RubyArrayTwoObject(RubyClass metaClass, RubyArrayTwoObject other) {
        this(metaClass, other.car, other.cdr);
    }

    @Override
    public final IRubyObject eltInternal(int index) {
        if (!packed()) return super.eltInternal(index);
        else if (index == 0) return car;
        else if (index == 1) return cdr;
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public final IRubyObject eltInternalSet(int index, IRubyObject value) {
        if (!packed()) return super.eltInternalSet(index, value);
        if (index == 0) return this.car = value;
        if (index == 1) return this.cdr = value;
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    protected void finishUnpack(IRubyObject nil) {
        car = cdr = nil;
    }

    @Override
    public RubyArrayNative<?> aryDup() {
        if (!packed()) return super.aryDup();
        return new RubyArrayTwoObject(getRuntime().getArray(), this);
    }

    @Override
    public IRubyObject rb_clear(ThreadContext context) {
        if (!packed()) return super.rb_clear(context);

        modifyCheck(context);

        // fail packing, but defer [] creation in case it is never needed
        car = cdr = context.nil;
        values = IRubyObject.NULL_ARRAY;
        realLength = 0;

        return this;
    }

    @Override
    public void copyInto(ThreadContext context, IRubyObject[] target, int start) {
        if (!packed()) {
            super.copyInto(context, target, start);
            return;
        }
        target[start] = car;
        target[start + 1] = cdr;
    }

    @Override
    public void copyInto(ThreadContext context, IRubyObject[] target, int start, int len) {
        if (!packed()) {
            super.copyInto(context, target, start, len);
            return;
        }
        if (len != 2) {
            unpack(context);
            super.copyInto(context, target, start, len);
            return;
        }
        target[start] = car;
        target[start + 1] = cdr;
    }

    @Override
    protected RubyArrayNative<?> dupImpl(Ruby runtime, RubyClass metaClass) {
        if (!packed()) return super.dupImpl(runtime, metaClass);
        return new RubyArrayTwoObject(metaClass, this);
    }

    @Override
    public boolean includes(ThreadContext context, IRubyObject item) {
        if (!packed()) return super.includes(context, item);

        if (equalInternal(context, car, item)) return true;
        if (equalInternal(context, cdr, item)) return true;

        return false;
    }

    @Override
    public int indexOf(Object element) {
        if (!packed()) return super.indexOf(element);

        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element);

            if (convertedElement.equals(car)) return 0;
            if (convertedElement.equals(cdr)) return 1;
        }
        return -1;
    }

    @Override
    protected IRubyObject inspectAry(ThreadContext context) {
        if (!packed()) return super.inspectAry(context);

        final Ruby runtime = context.runtime;
        RubyString str = RubyString.newStringLight(runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);
        str.cat((byte) '[');

        RubyString s1 = inspect(context, car);
        RubyString s2 = inspect(context, cdr);
        EncodingUtils.encAssociateIndex(str, s1.getEncoding());
        str.catWithCodeRange(s1);

        ByteList bytes = str.getByteList();
        bytes.append((byte) ',').append((byte) ' ');

        str.catWithCodeRange(s2);

        str.cat((byte) ']');

        return str;
    }

    @Override
    protected IRubyObject internalRotate(ThreadContext context, int cnt) {
        if (!packed()) return super.internalRotate(context, cnt);

        if (cnt % 2 == 1) return new RubyArrayTwoObject(context.runtime, cdr, car);
        return new RubyArrayTwoObject(context.runtime, car, cdr);
    }

    @Override
    protected IRubyObject internalRotateBang(ThreadContext context, int cnt) {
        if (!packed()) return super.internalRotateBang(context, cnt);

        modifyCheck(context);

        if (cnt % 2 == 1) {
            IRubyObject tmp = car;
            car = cdr;
            cdr = tmp;
        }

        return context.nil;
    }

    @Override
    public IRubyObject op_plus(ThreadContext context, IRubyObject obj) {
        if (!packed()) return super.op_plus(context, obj);
        var y = obj.convertToArray();
        if (y.isEmpty()) return new RubyArrayTwoObject(this);
        return super.op_plus(context, y);
    }

    @Override
    public IRubyObject replace(ThreadContext context, IRubyObject orig) {
        if (!packed()) return super.replace(context, orig);

        modifyCheck(context);

        var origArr = orig.convertToArray();

        if (this == orig) return this;

        if (origArr.size() == 2) {
            car = origArr.eltInternal(0);
            cdr = origArr.eltInternal(1);
            return this;
        }

        unpack(context);

        return super.replace(context, origArr);
    }

    @Override
    public IRubyObject reverse_bang(ThreadContext context) {
        if (!packed()) return super.reverse_bang(context);

        IRubyObject tmp = car;
        car = cdr;
        cdr = tmp;

        return this;
    }

    @Override
    protected RubyArray<?> safeReverse() {
        if (!packed()) return super.safeReverse();

        return new RubyArrayTwoObject(getMetaClass(), cdr, car);
    }

    @Override
    protected IRubyObject sortInternal(ThreadContext context, Block block) {
        if (!packed()) return super.sortInternal(context, block);

        IRubyObject car = this.car;
        IRubyObject cdr = this.cdr;

        IRubyObject ret = block.yieldArray(context, Create.newArray(context, car, cdr), null);
        //TODO: ary_sort_check should be done here
        int compare = RubyComparable.cmpint(context, ret, car, cdr);
        if (compare > 0) reverse_bang(context);
        return this;
    }

    @Override
    protected IRubyObject sortInternal(final ThreadContext context, boolean honorOverride) {
        if (!packed()) return super.sortInternal(context, honorOverride);

        Ruby runtime = context.runtime;

        // One check per specialized fast-path to make the check invariant.
        JavaSites.Array2Sites sites = sites(context);

        IRubyObject o1 = car;
        IRubyObject o2 = cdr;

        int compare;
        if (isFixnumBypass(runtime, sites, honorOverride) && o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
            compare = compareFixnums((RubyFixnum) o1, (RubyFixnum) o2);
        } else if (isStringBypass(runtime, sites, honorOverride) && o1 instanceof RubyString && o2 instanceof RubyString) {
            compare = ((RubyString) o1).op_cmp((RubyString) o2);
        } else {
            compare = compareOthers(context, o1, o2);
        }

        if (compare > 0) reverse_bang(context);

        return this;
    }

    private boolean isStringBypass(Ruby runtime, JavaSites.Array2Sites sites, boolean honorOverride) {
        return !honorOverride || sites.op_cmp_string.isBuiltin(runtime.getString());
    }

    private boolean isFixnumBypass(Ruby runtime, JavaSites.Array2Sites sites, boolean honorOverride) {
        return !honorOverride || sites.op_cmp_fixnum.isBuiltin(runtime.getFixnum());
    }

    @Deprecated(since = "10.0.0.0")
    protected void storeInternal(final int index, final IRubyObject value) {
        storeInternal(getCurrentContext(), index, value);
    }

    @Override
    protected void storeInternal(ThreadContext context, final int index, final IRubyObject value) {
        if (packed()) {
            switch (index) {
                case 0: car = value; return;
                case 1: cdr = value; return;
            }
            unpack(context);
        }

        super.storeInternal(context, index, value);
    }

    @Override
    public IRubyObject subseq(RubyClass metaClass, long beg, long len, boolean light) {
        if (!packed()) return super.subseq(metaClass, beg, len, light);

        Ruby runtime = getRuntime();

        if (beg > 2 || beg < 0 || len < 0) return runtime.getNil();

        if (len == 0 || beg == 2) return RubyArray.newEmptyArray(runtime, metaClass);

        if (beg == 0) {
            if (len == 1) return new RubyArrayOneObject(metaClass, car);
            return new RubyArrayTwoObject(metaClass, this);
        }

        // beg == 1, len >= 1
        return new RubyArrayOneObject(metaClass, cdr);
    }

    @Override
    public IRubyObject[] toJavaArray(ThreadContext context) {
        if (!packed()) return super.toJavaArray(context);

        return arrayOf(car, cdr);
    }

    @Override
    public IRubyObject uniq(ThreadContext context) {
        if (!packed()) return super.uniq(context);

        IRubyObject car = this.car;
        IRubyObject cdr = this.cdr;

        if (invokedynamic(context, car, MethodNames.HASH).equals(invokedynamic(context, cdr, MethodNames.HASH)) &&
                (car == cdr || invokedynamic(context, car, MethodNames.EQL, cdr).isTrue())) {
            // Use cdr because it would have been inserted into RubyArray#uniq's RubyHash last
            return new RubyArrayOneObject(getMetaClass(), cdr);
        } else {
            return new RubyArrayTwoObject(this);
        }
    }

    @Override
    public RubyArray<?> collectArray(ThreadContext context, Block block) {
        if (!packed()) return super.collectArray(context, block);
        if (!block.isGiven()) return makeShared();

        IRubyObject newCar = block.yieldNonArray(context, this.car, null);

        if (realLength == 2) { // no size change, yield last elt and return
            return new RubyArrayTwoObject(context.runtime, newCar, block.yieldNonArray(context, cdr, null));
        }

        // size has changed, unpack and continue with loop form
        unpack(context);

        int currentLength = this.realLength;
        if (currentLength == 0) return Create.newEmptyArray(context);

        IRubyObject[] arr = IRubyObject.array(currentLength);
        arr[0] = newCar;

        int i = 1;
        for (; i < this.realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
            // See JRUBY-5434
            safeArraySet(context, arr, i, block.yieldNonArray(context, eltOk(i), null)); // arr[i] = ...
        }

        // use iteration count as new size in case something was deleted along the way
        return newArrayMayCopy(context.runtime, arr, 0, i);
    }

    @Override
    protected RubyArrayNative<?> makeShared() {
        if (!packed()) return super.makeShared();

        return new RubyArrayTwoObject(this);
    }

    private static final JavaSites.Array2Sites sites(ThreadContext context) {
        return context.sites.Array2;
    }
}
