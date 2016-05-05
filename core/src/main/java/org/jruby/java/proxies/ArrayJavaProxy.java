package org.jruby.java.proxies;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyRange;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.util.ArrayUtils;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaArray;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public final class ArrayJavaProxy extends JavaProxy {

    private final JavaUtil.JavaConverter converter;

    public ArrayJavaProxy(Ruby runtime, RubyClass klazz, Object array) {
        this(runtime, klazz, array, JavaUtil.getJavaConverter(array.getClass().getComponentType()));
    }

    public ArrayJavaProxy(Ruby runtime, RubyClass klazz, Object array, JavaUtil.JavaConverter converter) {
        super(runtime, klazz, array);
        this.converter = converter;
    }

    public static RubyClass createArrayJavaProxy(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyClass arrayJavaProxy = runtime.defineClass("ArrayJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        RubyClass singleton = arrayJavaProxy.getSingletonClass();
        singleton.addMethod("new", new ArrayNewMethod(singleton, Visibility.PUBLIC));

        arrayJavaProxy.defineAnnotatedMethods(ArrayJavaProxy.class);
        arrayJavaProxy.includeModule(runtime.getEnumerable());

        return arrayJavaProxy;
    }

    static ArrayJavaProxy newArray(final Ruby runtime, final Class<?> elementType, final int... dimensions) {
        final Object array;
        try {
            array = Array.newInstance(elementType, dimensions);
        }
        catch (IllegalArgumentException e) {
            throw runtime.newArgumentError("can not create " + dimensions.length + " dimensional array");
        }
        return new ArrayJavaProxy(runtime, Java.getProxyClassForObject(runtime, array), array);
    }

    public JavaArray getJavaArray() {
        JavaArray javaArray = (JavaArray) dataGetStruct();

        if (javaArray == null) {
            javaArray = new JavaArray(getRuntime(), getObject());
            dataWrapStruct(javaArray);
        }

        return javaArray;
    }

    @JRubyMethod(name = {"length", "size"})
    public RubyFixnum length(ThreadContext context) {
        return context.runtime.newFixnum( Array.getLength( getObject() ) );
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return context.runtime.newBoolean( Array.getLength( getObject() ) == 0 );
    }

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        if ( arg instanceof RubyRange ) return arrayRange(context, (RubyRange) arg);
        final int i = convertArrayIndex(arg);
        return ArrayUtils.arefDirect(context.runtime, getObject(), converter, i);
    }

    public Object get(final int index) {
        return Array.get(getObject(), index);
    }

    public void set(final int index, final Object value) {
        Array.set(getObject(), index, value);
    }

    @JRubyMethod(name = "[]", required = 1, rest = true)
    public final IRubyObject op_aref(ThreadContext context, IRubyObject[] args) {
        if ( args.length == 1 ) return op_aref(context, args[0]);
        return getRange(context, args);
    }

    @JRubyMethod(name = "[]=")
    public final IRubyObject op_aset(ThreadContext context, IRubyObject index, IRubyObject value) {
        final int i = convertArrayIndex(index);
        return ArrayUtils.asetDirect(context.runtime, getObject(), converter, i, value);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true)
    public final IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        return dig(context, args, 0);
    }

    final IRubyObject dig(ThreadContext context, IRubyObject[] args, int idx) {
        final IRubyObject val = at(context, args[idx++]);
        return idx == args.length ? val : RubyObject.dig(context, val, args, idx);
    }

    private static int convertArrayIndex(final IRubyObject index) {
        if ( index instanceof JavaProxy ) {
            return (Integer) index.toJava(Integer.class);
        }
        return RubyNumeric.num2int(index);
    }

    @JRubyMethod
    public IRubyObject at(ThreadContext context, IRubyObject index) {
        return at(context, convertArrayIndex(index));
    }

    private final IRubyObject at(ThreadContext context, int i) {
        final Ruby runtime = context.runtime;
        final Object array = getObject();
        final int length = Array.getLength(array);

        if ( i < 0 ) i = i + length;

        if ( i >= 0 && i < length ) {
            return ArrayUtils.arefDirect(runtime, array, converter, i);
        }
        return context.nil;
    }

    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        final Object array = getObject();
        if ( other instanceof ArrayJavaProxy ) {
            final Object otherArray = ((ArrayJavaProxy) other).getObject();
            final Class<?> componentType = array.getClass().getComponentType();
            if ( componentType.isAssignableFrom( otherArray.getClass().getComponentType() ) ) {
                return ArrayUtils.concatArraysDirect(context, array, otherArray);
            }
        }
        return ArrayUtils.concatArraysDirect(context, array, other);
    }

    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        final Ruby runtime = context.runtime;
        final Object array = getObject();
        final int length = Array.getLength(array);

        for ( int i = 0; i < length; i++ ) {
            IRubyObject element = ArrayUtils.arefDirect(runtime, array, converter, i);
            block.yield(context, element);
        }
        return this;
    }

    @JRubyMethod(name = {"to_a", "to_ary"})
    public RubyArray to_a(ThreadContext context) {
        final Object array = getObject();
        return JavaUtil.convertJavaArrayToRubyWithNesting(context, array);
    }

    @JRubyMethod(name = {"component_type"})
    public IRubyObject component_type(ThreadContext context) {
        Class<?> componentType = getObject().getClass().getComponentType();
        final JavaClass javaClass = JavaClass.get(context.runtime, componentType);
        return Java.getProxyClass(context.runtime, javaClass);
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        return RubyString.newString(context.runtime, arrayToString());
    }

    @Override
    public String toString() {
        return arrayToString().toString();
    }

    private StringBuilder arrayToString() {
        final StringBuilder buffer = new StringBuilder(24);
        Class<?> componentClass = getObject().getClass().getComponentType();

        buffer.append(componentClass.getName());

        if (componentClass.isPrimitive()) {
            switch (componentClass.getName().charAt(0)) {
                case 'b':
                    if (componentClass == byte.class) buffer.append(Arrays.toString((byte[])getObject()));
                    else /* if (componentClass == boolean.class) */ buffer.append(Arrays.toString((boolean[])getObject()));
                    break;
                case 's':
                    /* if (componentClass == short.class) */ buffer.append(Arrays.toString((short[])getObject()));
                    break;
                case 'c':
                    /* if (componentClass == char.class) */ buffer.append(Arrays.toString((char[])getObject()));
                    break;
                case 'i':
                    /* if (componentClass == int.class) */ buffer.append(Arrays.toString((int[])getObject()));
                    break;
                case 'l':
                    /* if (componentClass == long.class) */ buffer.append(Arrays.toString((long[])getObject()));
                    break;
                case 'f':
                    /* if (componentClass == float.class) */ buffer.append(Arrays.toString((float[])getObject()));
                    break;
                case 'd':
                    /* if (componentClass == double.class) */ buffer.append(Arrays.toString((double[])getObject()));
                    break;
            }
        } else {
            buffer.append(Arrays.toString((Object[]) getObject()));
        }

        return buffer.append('@').append(Integer.toHexString(inspectHashCode()));
    }

    @Override
    @JRubyMethod(name = "==")
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        if ( other instanceof RubyArray ) {
            // we respond_to? to_ary thus shall handle [1].to_java == [1]
            return context.runtime.newBoolean( equalsRubyArray((RubyArray) other) );
        }
        return eql_p(context, other);
    }

    private boolean equalsRubyArray(final RubyArray rubyArray) {
        final Object thisArray = this.getObject();
        final int len = rubyArray.size();
        if ( len != Array.getLength(thisArray) ) return false;
        final Class<?> componentType = thisArray.getClass().getComponentType();
        for ( int i = 0; i < len; i++ ) {
            final Object ruby = rubyArray.eltInternal(i).toJava(componentType);
            final Object elem = Array.get(thisArray, i);
            if ( ruby == null ) return elem == null;
            if ( ! ruby.equals(elem) ) return false;
        }
        return true;
    }

    @JRubyMethod(name = "eql?")
    public RubyBoolean eql_p(ThreadContext context, IRubyObject obj) {
        boolean equals = false;
        if ( obj instanceof ArrayJavaProxy ) {
            final ArrayJavaProxy that = (ArrayJavaProxy) obj;
            equals = arraysEquals(this.getObject(), that.getObject());
        }
        else if ( obj.getClass().isArray() ) {
            equals = arraysEquals(getObject(), obj);
        }
        return context.runtime.newBoolean(equals);
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof ArrayJavaProxy ) {
            final ArrayJavaProxy that = (ArrayJavaProxy) obj;
            final Object thisArray = this.getObject();
            final Object thatArray = that.getObject();
            return arraysEquals(thisArray, thatArray);
        }
        return false;
    }

    private static boolean arraysEquals(final Object thisArray, final Object thatArray) {
        final Class<?> componentType = thisArray.getClass().getComponentType();
        if ( ! componentType.equals(thatArray.getClass().getComponentType()) ) {
            return false;
        }
        if ( componentType.isPrimitive() ) {
            switch ( componentType.getName().charAt(0) ) {
                case 'b':
                    if (componentType == byte.class) return Arrays.equals((byte[]) thisArray, (byte[]) thatArray);
                    else /* if (componentType == boolean.class) */ return Arrays.equals((boolean[]) thisArray, (boolean[]) thatArray);
                case 's':
                    /* if (componentType == short.class) */ return Arrays.equals((short[]) thisArray, (short[]) thatArray);
                case 'c':
                    /* if (componentType == char.class) */ return Arrays.equals((char[]) thisArray, (char[]) thatArray);
                case 'i':
                    /* if (componentType == int.class) */ return Arrays.equals((int[]) thisArray, (int[]) thatArray);
                case 'l':
                    /* if (componentType == long.class) */ return Arrays.equals((long[]) thisArray, (long[]) thatArray);
                case 'f':
                    /* if (componentType == float.class) */ return Arrays.equals((float[]) thisArray, (float[]) thatArray);
                case 'd':
                    /* if (componentType == double.class) */ return Arrays.equals((double[]) thisArray, (double[]) thatArray);
            }
        }
        return Arrays.equals((Object[]) thisArray, (Object[]) thatArray);
    }

    @Override
    @JRubyMethod
    public RubyFixnum hash() {
        return getRuntime().newFixnum( hashCode() );
    }

    @Override
    public int hashCode() {
        final Object array = getObject();
        final Class<?> componentType = array.getClass().getComponentType();
        if ( componentType.isPrimitive() ) {
            switch ( componentType.getName().charAt(0) ) {
                case 'b':
                    if (componentType == byte.class) return 11 * Arrays.hashCode((byte[]) array);
                    else /* if (componentType == boolean.class) */ return 11 * Arrays.hashCode((boolean[]) array);
                case 's':
                    /* if (componentType == short.class) */ return 11 * Arrays.hashCode((short[]) array);
                case 'c':
                    /* if (componentType == char.class) */ return 11 * Arrays.hashCode((char[]) array);
                case 'i':
                    /* if (componentType == int.class) */ return 11 * Arrays.hashCode((int[]) array);
                case 'l':
                    /* if (componentType == long.class) */ return 11 * Arrays.hashCode((long[]) array);
                case 'f':
                    /* if (componentType == float.class) */ return 11 * Arrays.hashCode((float[]) array);
                case 'd':
                    /* if (componentType == double.class) */ return 11 * Arrays.hashCode((double[]) array);
            }
        }
        return 11 * Arrays.hashCode((Object[]) array);
    }

    @Override
    public IRubyObject dup() {
        final Ruby runtime = getRuntime();

        RubyObject dup = new ArrayJavaProxy(runtime, getMetaClass(), cloneObject(), converter);

        if (isTaint()) dup.setTaint(true);
        initCopy(dup, this, "initialize_dup");

        return dup;
    }

    private static void initCopy(IRubyObject clone, IRubyObject original, String method) {
        original.copySpecialInstanceVariables(clone);
        if (original.hasVariables()) clone.syncVariables(original);
    }

    @Override
    @JRubyMethod(name = "clone")
    public IRubyObject rbClone() {
        final Ruby runtime = getRuntime();

        RubyObject clone = new ArrayJavaProxy(runtime, getMetaClass(), cloneObject(), converter);
        clone.setMetaClass(getSingletonClassClone());

        if (isTaint()) clone.setTaint(true);
        initCopy(clone, this, "initialize_clone");
        if (isFrozen()) clone.setFrozen(true);

        return clone;
    }

    @Override
    protected Object cloneObject() {
        final Object array = getObject();
        final Class<?> componentType = array.getClass().getComponentType();
        if ( componentType.isPrimitive() ) {
            switch ( componentType.getName().charAt(0) ) {
                case 'b':
                    if (componentType == byte.class) return ((byte[]) array).clone();
                    else /* if (componentType == boolean.class) */ return ((boolean[]) array).clone();
                case 's':
                    /* if (componentType == short.class) */ return ((short[]) array).clone();
                case 'c':
                    /* if (componentType == char.class) */ return ((char[]) array).clone();
                case 'i':
                    /* if (componentType == int.class) */ return ((int[]) array).clone();
                case 'l':
                    /* if (componentType == long.class) */ return ((long[]) array).clone();
                case 'f':
                    /* if (componentType == float.class) */ return ((float[]) array).clone();
                case 'd':
                    /* if (componentType == double.class) */ return ((double[]) array).clone();
            }
        }
        return ((Object[]) array).clone();
    }

    public IRubyObject getRange(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1) {
            return getRange(context, args[0]);
        }
        if (args.length == 2) {
            return getRange(context, args[0], args[1]);
        }
        throw context.runtime.newArgumentError(args.length, 1);
    }

    public IRubyObject getRange(ThreadContext context, IRubyObject arg0) {
        if ( arg0 instanceof RubyRange ) {
            return arrayRange(context, (RubyRange) arg0);
        }
        throw context.runtime.newTypeError(arg0, context.runtime.getRange());
    }

    private IRubyObject arrayRange(final ThreadContext context, final RubyRange range) {
        final Object array = getObject();
        final int arrayLength = Array.getLength( array );

        final IRubyObject rFirst = range.first(context);
        final IRubyObject rLast = range.last(context);
        if ( rFirst instanceof RubyFixnum && rLast instanceof RubyFixnum ) {
            int first = (int) ((RubyFixnum) rFirst).getLongValue();
            int last = (int) ((RubyFixnum) rLast).getLongValue();

            first = first >= 0 ? first : arrayLength + first;
            last = last >= 0 ? last : arrayLength + last;

            int newLength = last - first;
            if ( range.exclude_end_p().isFalse() ) newLength += 1;

            if ( newLength <= 0 ) {
                return ArrayUtils.emptyJavaArrayDirect(context, array.getClass().getComponentType());
            }

            return ArrayUtils.javaArraySubarrayDirect(context, array, first, newLength);
        }
        throw context.runtime.newTypeError("only Fixnum ranges supported");
    }

    public IRubyObject getRange(ThreadContext context, IRubyObject first, IRubyObject length) {
        return arrayRange(context, first, length);
    }

    private IRubyObject arrayRange(final ThreadContext context,
        final IRubyObject rFirst, final IRubyObject rLength) {
        final Object array = getObject();
        final int arrayLength = Array.getLength( array );

        if ( rFirst instanceof RubyFixnum && rLength instanceof RubyFixnum ) {
            int first = (int) ((RubyFixnum) rFirst).getLongValue();
            int length = (int) ((RubyFixnum) rLength).getLongValue();

            if ( length > arrayLength ) {
                throw context.runtime.newIndexError("length specifed is longer than array");
            }
            if ( length <= 0 ) {
                return ArrayUtils.emptyJavaArrayDirect(context, array.getClass().getComponentType());
            }

            first = first >= 0 ? first : arrayLength + first;
            return ArrayUtils.javaArraySubarrayDirect(context, array, first, length);
        }
        throw context.runtime.newTypeError("only Fixnum ranges supported");
    }

    private static final class ArrayNewMethod extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne {

        private final DynamicMethod newMethod;

        ArrayNewMethod(RubyModule implClass, Visibility visibility) {
            this(implClass, visibility, implClass.searchMethod("new"));
        }

        public ArrayNewMethod(RubyModule implClass, Visibility visibility, DynamicMethod oldNew) {
            super(implClass, visibility);
            this.newMethod = oldNew;
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            final Ruby runtime = context.runtime;

            if ( ! ( arg0 instanceof JavaArray ) ) {
                throw runtime.newTypeError(arg0, runtime.getJavaSupport().getJavaArrayClass());
            }

            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy");
            proxy.dataWrapStruct(arg0);
            return proxy;
        }

    }
}
