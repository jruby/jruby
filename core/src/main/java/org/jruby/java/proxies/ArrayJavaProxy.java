package org.jruby.java.proxies;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.util.ArrayUtils;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ConvertBytes;
import org.jruby.util.RubyStringBuilder;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.indexError;
import static org.jruby.api.Error.typeError;
import static org.jruby.javasupport.ext.JavaLang.Character.inspectCharValue;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.util.Inspector.*;

public final class ArrayJavaProxy extends JavaProxy {

    private final JavaUtil.JavaConverter converter;

    public ArrayJavaProxy(Ruby runtime, RubyClass klazz, Object array) {
        this(runtime, klazz, array, JavaUtil.getJavaConverter(array.getClass().getComponentType()));
    }

    public ArrayJavaProxy(Ruby runtime, RubyClass klazz, Object array, JavaUtil.JavaConverter converter) {
        super(runtime, klazz, array);
        this.converter = converter;
    }

    public static RubyClass createArrayJavaProxy(ThreadContext context, RubyClass JavaProxy, RubyModule Enumerable) {
        return defineClass(context, "ArrayJavaProxy", JavaProxy, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, ArrayJavaProxy.class).
                include(context, Enumerable).
                tap(c -> c.singletonClass(context).addMethod(context, "new", new ArrayNewMethod(c.singletonClass(context), Visibility.PUBLIC)));
    }

    @Deprecated(since = "10.0")
    public static ArrayJavaProxy newArray(final Ruby runtime, final Class<?> elementType, final int... dimensions) {
        return newArray(runtime.getCurrentContext(), elementType, dimensions);
    }

    public static ArrayJavaProxy newArray(ThreadContext context, final Class<?> elementType, final int... dimensions) {
        final Object array;
        try {
            array = Array.newInstance(elementType, dimensions);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, "can not create " + dimensions.length + " dimensional array");
        }
        return new ArrayJavaProxy(context.runtime, Java.getProxyClassForObject(context, array), array);
    }

    @Override
    @Deprecated
    protected org.jruby.javasupport.JavaArray asJavaObject(final Object array) {
        return new org.jruby.javasupport.JavaArray(getCurrentContext().runtime, array);
    }

    @Deprecated
    public final org.jruby.javasupport.JavaArray getJavaArray() {
        return asJavaObject(this.object);
    }

    public Object get(final int index) {
        return Array.get(getObject(), index);
    }

    public void set(final int index, final Object value) {
        Array.set(getObject(), index, value);
    }

    public IRubyObject setValue(final Ruby runtime, final int index, final IRubyObject value) {
        return ArrayUtils.asetDirect(runtime, getObject(), converter, index, value);
    }

    public final int length() { return Array.getLength( getObject() ); }

    @JRubyMethod(name = {"length", "size"})
    public RubyFixnum length(ThreadContext context) {
        return asFixnum(context, length());
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return asBoolean(context,  length() == 0 );
    }

    @JRubyMethod(name = "[]")
    public final IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        if ( arg instanceof RubyRange ) return arrayRange(context, (RubyRange) arg);
        final int i = convertArrayIndex(context, arg);
        return ArrayUtils.arefDirect(context.runtime, getObject(), converter, i);
    }

    @JRubyMethod(name = "[]", required = 1, rest = true, checkArity = false)
    public final IRubyObject op_aref(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        if ( argc == 1 ) return op_aref(context, args[0]);
        return getRange(context, args);
    }

    @JRubyMethod(name = "[]=")
    public final IRubyObject op_aset(ThreadContext context, IRubyObject index, IRubyObject value) {
        return setValue(context.runtime, convertArrayIndex(context, index), value);
    }

    @JRubyMethod(name = { "include?", "member?" }) // Enumerable override
    public IRubyObject include_p(ThreadContext context, IRubyObject obj) {
        final Object array = getObject();

        final Class<?> componentClass = array.getClass().getComponentType();

        if ( componentClass.isPrimitive() ) {
            switch (componentClass.getName().charAt(0)) {
                case 'b':
                    if (componentClass == byte.class) return asBoolean(context,  includes(context, (byte[]) array, obj) );
                    else /* if (componentClass == boolean.class) */ return asBoolean(context,  includes(context, (boolean[]) array, obj) );
                    // break;
                case 's':
                    /* if (componentClass == short.class) */ return asBoolean(context,  includes(context, (short[]) array, obj) );
                    // break;
                case 'c':
                    /* if (componentClass == char.class) */ return asBoolean(context,  includes(context, (char[]) array, obj) );
                    // break;
                case 'i':
                    /* if (componentClass == int.class) */ return asBoolean(context,  includes(context, (int[]) array, obj) );
                    // break;
                case 'l':
                    /* if (componentClass == long.class) */ return asBoolean(context,  includes(context, (long[]) array, obj) );
                    // break;
                case 'f':
                    /* if (componentClass == float.class) */ return asBoolean(context,  includes(context, (float[]) array, obj) );
                    // break;
                case 'd':
                    /* if (componentClass == double.class) */ return asBoolean(context,  includes(context, (double[]) array, obj) );
                    // break;
            }
        }
        return asBoolean(context,  includes(context, (Object[]) array, obj) );
    }

    private boolean includes(final ThreadContext context, final Object[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        final Ruby runtime = context.runtime;
        for ( int i = 0; i < len; i++ ) {
            IRubyObject value = JavaUtil.convertJavaArrayElementToRuby(runtime, converter, array, i);
            if ( equalInternal(context, value, obj) ) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final byte[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        if ( obj instanceof RubyFixnum fix) {
            final long objVal = fix.getValue();
            if ( objVal < Byte.MIN_VALUE || objVal > Byte.MAX_VALUE ) return false;

            for (byte b : array) {
                if ((byte) objVal == b) return true;
            }
            return false;
        }
        for (byte b : array) {
            if (equalInternal(context, asFixnum(context, b), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final short[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        if (obj instanceof RubyFixnum fix) {
            final long objVal = fix.getValue();
            if ( objVal < Short.MIN_VALUE || objVal > Short.MAX_VALUE ) return false;

            for (short value : array) {
                if ((short) objVal == value) return true;
            }
            return false;
        }
        for (short item : array) {
            if (equalInternal(context, asFixnum(context, item), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final int[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        if (obj instanceof RubyFixnum fix) {
            final long objVal = fix.getValue();
            if ( objVal < Integer.MIN_VALUE || objVal > Integer.MAX_VALUE ) return false;

            for (int j : array) {
                if ((int) objVal == j) return true;
            }
            return false;
        }
        for (int j : array) {
            if (equalInternal(context, asFixnum(context, j), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final long[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        if ( obj instanceof RubyFixnum fix) {
            final long objVal = fix.getValue();

            for (long l : array) {
                if (objVal == l) return true;
            }
            return false;
        }
        for (long l : array) {
            if (equalInternal(context, asFixnum(context, l), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final char[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        if (obj instanceof RubyFixnum fix) {
            final long objVal = fix.getValue();
            if ( objVal < Character.MIN_VALUE || objVal > Character.MAX_VALUE ) return false;

            for (char c : array) {
                if ((int) objVal == c) return true;
            }
            return false;
        }
        for (char c : array) {
            if (equalInternal(context,  asFixnum(context, c), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final boolean[] array, final IRubyObject obj) {
        final int len = array.length;
        if ( len == 0 ) return false;
        if ( obj instanceof RubyBoolean bool) {
            final boolean objVal = bool.isTrue();

            for (boolean b : array) {
                if (objVal == b) return true;
            }
            return false;
        }
        for (boolean b : array) {
            if (equalInternal(context, asBoolean(context, b), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final float[] array, final IRubyObject obj) {
        final int len = array.length;
        if (len == 0) return false;
        if (obj instanceof RubyFloat flote) {
            final double objVal = flote.asDouble(context);

            for (float v : array) {
                if ((float) objVal == v) return true;
            }
            return false;
        }
        for (float v : array) {
            if (equalInternal(context, asFloat(context, v), obj)) return true;
        }
        return false;
    }

    private boolean includes(final ThreadContext context, final double[] array, final IRubyObject obj) {
        final int len = array.length;
        if (len == 0) return false;
        if (obj instanceof RubyFloat flote) {
            final double objVal = flote.asDouble(context);

            for (double v : array) {
                if (objVal == v) return true;
            }
            return false;
        }
        for (double v : array) {
            if (equalInternal(context, asFloat(context, v), obj)) return true;
        }
        return false;
    }

    @JRubyMethod(name = "first") // Enumerable override
    public IRubyObject first(ThreadContext context) {
        final Object array = getObject();
        // Enumerable does not raise when length == 0 (compatibility)
        if ( Array.getLength(array) == 0 ) return context.nil;
        return JavaUtil.convertJavaArrayElementToRuby(context.runtime, converter, array, 0);
    }

    @JRubyMethod(name = "first") // Enumerable override
    public IRubyObject first(ThreadContext context, IRubyObject count) {
        final Object array = getObject();
        int len = toInt(context, count);
        int size = Array.getLength(array); if ( len > size ) len = size;

        final Ruby runtime = context.runtime;
        if ( len == 0 ) return RubyArray.newEmptyArray(runtime);
        final IRubyObject[] ary = new IRubyObject[len];
        for ( int i = 0; i < len; i++ ) {
            ary[i] = JavaUtil.convertJavaArrayElementToRuby(runtime, converter, array, i);
        }
        return RubyArray.newArrayNoCopy(runtime, ary);
    }

    @JRubyMethod(name = "last") // ~ Array#last
    public IRubyObject last(ThreadContext context) {
        final Object array = getObject();
        final int len = Array.getLength(array);
        if ( len == 0 ) return context.nil;
        return JavaUtil.convertJavaArrayElementToRuby(context.runtime, converter, array, len - 1);
    }

    @JRubyMethod(name = "last") // ~ Array#last
    public IRubyObject last(ThreadContext context, IRubyObject count) {
        final Object array = getObject();

        int len = toInt(context, count);
        int size = Array.getLength(array);
        int start = size - len; if ( start < 0 ) start = 0;
        int end = start + len; if ( end > size ) end = size;
        len = end - start;

        final Ruby runtime = context.runtime;
        if ( len == 0 ) return RubyArray.newEmptyArray(runtime);
        final IRubyObject[] ary = new IRubyObject[len];
        for ( int i = 0; i < len; i++ ) {
            ary[i] = JavaUtil.convertJavaArrayElementToRuby(runtime, converter, array, i + start);
        }
        return RubyArray.newArrayNoCopy(runtime, ary);
    }

    @JRubyMethod(name = "count") // @override Enumerable#count
    public IRubyObject count(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return asFixnum(context, length());

        final Object array = getObject();
        int count = 0;
        for ( int i = 0; i < Array.getLength(array); i++ ) {
            IRubyObject next = JavaUtil.convertJavaArrayElementToRuby(context.runtime, converter, array, i);
            if (block.yield(context, next).isTrue()) count++;
        }
        return asFixnum(context, count);
    }

    @JRubyMethod(name = "count") // @override Enumerable#count
    public IRubyObject count(final ThreadContext context, final IRubyObject obj, final Block unused) {
        // unused block due DescriptorInfo not (yet) supporting if a method receives block and an override doesn't
        final Object array = getObject(); int count = 0;
        for ( int i = 0; i < Array.getLength(array); i++ ) {
            // NOTE: could be former improved by special case handling primitive arrays and == ...
            IRubyObject next = JavaUtil.convertJavaArrayElementToRuby(context.runtime, converter, array, i);
            if ( RubyObject.equalInternal(context, next, obj) ) count++;
        }
        return asFixnum(context, count);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true, checkArity = false)
    public final IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, -1);

        return dig(context, args, 0);
    }

    final IRubyObject dig(ThreadContext context, IRubyObject[] args, int idx) {
        final IRubyObject val = at(context, args[idx++]);
        return idx == args.length ? val : RubyObject.dig(context, val, args, idx);
    }

    private static int convertArrayIndex(ThreadContext context, final IRubyObject index) {
        return index instanceof JavaProxy ?
                index.toJava(Integer.class) :
                toInt(context, index);
    }

    @JRubyMethod
    public IRubyObject at(ThreadContext context, IRubyObject index) {
        return at(context, convertArrayIndex(context, index));
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
        if ( ! block.isGiven() ) { // ... Enumerator.new(self, :each)
            return enumeratorizeWithSize(context, this, "each", ArrayJavaProxy::size);
        }

        final Object array = getObject();
        final int length = Array.getLength(array);

        for ( int i = 0; i < length; i++ ) {
            IRubyObject element = ArrayUtils.arefDirect(runtime, array, converter, i);
            block.yield(context, element);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject each_with_index(final ThreadContext context, final Block block) {
        final Ruby runtime = context.runtime;
        if ( ! block.isGiven() ) { // ... Enumerator.new(self, :each_with_index)
            return enumeratorizeWithSize(context, this, "each_with_index", ArrayJavaProxy::size);
        }

        final boolean twoArguments = block.getSignature().isTwoArguments();
        final Object array = getObject();
        final int length = Array.getLength(array);

        for ( int i = 0; i < length; i++ ) {
            IRubyObject element = ArrayUtils.arefDirect(runtime, array, converter, i);
            final RubyInteger index = asFixnum(context, i);

            if (twoArguments) {
                block.yieldSpecific(context, element, index);
            } else {
                block.yield(context, Create.newArray(context, element, index));
            }
        }
        return this;
    }

    @JRubyMethod(name = { "to_a", "entries" }, alias = "to_ary")
    public RubyArray to_a(ThreadContext context) {
        final Object array = getObject();
        return JavaUtil.convertJavaArrayToRubyWithNesting(context, array);
    }

    public Class<?> getComponentType() {
        return getObject().getClass().getComponentType();
    }

    @JRubyMethod(name = "component_type")
    public IRubyObject component_type(ThreadContext context) {
        return Java.getProxyClass(context, getComponentType());
    }

    private static final byte[] END_BRACKET_COLON_SPACE = new byte[] { ']', ':', ' ' };

    // #<Java::long[3]: [1, 2, 0]>
    // #<Java::int[0]: []>
    // #<Java::JavaLang::String[1]: ["foo"]>
    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        final Ruby runtime = context.runtime;

        final Class<?> componentClass = getObject().getClass().getComponentType();
        if (componentClass.isPrimitive()) {
            return inspectPrimitiveArray(runtime, componentClass);
        }

        final Object[] ary = (Object[]) getObject();

        RubyModule type = Java.getProxyClass(context, componentClass);
        RubyString buf = inspectPrefixTypeOnly(context, type);
        RubyStringBuilder.cat(runtime, buf, BEG_BRACKET); // [
        RubyStringBuilder.cat(runtime, buf, ConvertBytes.intToCharBytes(ary.length));
        RubyStringBuilder.cat(runtime, buf, END_BRACKET_COLON_SPACE); // ]:

        if (ary.length == 0) {
            RubyStringBuilder.cat(runtime, buf, EMPTY_ARRAY_BL);
        } else if (runtime.isInspecting(ary)) {
            RubyStringBuilder.cat(runtime, buf, RECURSIVE_ARRAY_BL);
        } else {
            try {
                runtime.registerInspecting(ary);

                RubyStringBuilder.cat(runtime, buf, BEG_BRACKET); // [
                for (int i = 0; i < ary.length; i++) {
                    RubyString s = JavaUtil.inspectObject(context, ary[i]);
                    if (i > 0) {
                        RubyStringBuilder.cat(runtime, buf, COMMA_SPACE); // ,
                    } else {
                        buf.setEncoding(s.getEncoding());
                    }
                    buf.catWithCodeRange(s);
                }
                RubyStringBuilder.cat(runtime, buf, END_BRACKET); // ]
            } finally {
                runtime.unregisterInspecting(ary);
            }
        }

        RubyStringBuilder.cat(runtime, buf, GT); // >
        return buf;
    }

    private RubyString inspectPrimitiveArray(final Ruby runtime, final Class<?> componentClass) {
        final int len = Array.getLength(getObject());

        final StringBuilder buffer = new StringBuilder(24);
        final String name = componentClass.getName();
        buffer.append("#<Java::").append(name).append('[').append(len).append("]: ");
        switch (name.charAt(0)) {
            case 'b':
                if (componentClass == byte.class) buffer.append(Arrays.toString((byte[])getObject()));
                else /* if (componentClass == boolean.class) */ buffer.append(Arrays.toString((boolean[])getObject()));
                break;
            case 's':
                /* if (componentClass == short.class) */ buffer.append(Arrays.toString((short[])getObject()));
                break;
            case 'c':
                /* if (componentClass == char.class) */
                return inspectCharArrayPart(runtime, buffer, (char[])getObject(), len);
            case 'i':
                /* if (componentClass == int.class) */ buffer.append(Arrays.toString((int[])getObject()));
                ///* if (componentClass == int.class) */ toString(buffer, (int[])getObject());
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
        return RubyString.newUSASCIIString(runtime, buffer.append('>').toString());
    }

    // NOTE: special case as we want to inspect like a Character wrapper e.g. ['', 'a']
    private static RubyString inspectCharArrayPart(final Ruby runtime, final StringBuilder buffer, final char[] ary, final int len) {
        buffer.append('[');
        if (len > 0) {
            for (int i = 0; ; i++) {
                inspectCharValue(buffer, ary[i]);
                if (i == len - 1) break;
                buffer.append(", ");
            }
        }
        buffer.append(']');
        return RubyString.newString(runtime, buffer.append('>'));
    }

    // long[1, 2, 0]
    // int[]
    // java.lang.String["foo"]
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(24);
        final Class<?> componentClass = getObject().getClass().getComponentType();
        final String name = componentClass.getName();
        buffer.append(name);

        if (componentClass.isPrimitive()) {
            switch (name.charAt(0)) {
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
                    ///* if (componentClass == int.class) */ toString(buffer, (int[])getObject());
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

        return buffer.toString();
    }

    @Override
    @JRubyMethod(name = "==")
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        if ( other instanceof RubyArray ) {
            // we respond_to? to_ary thus shall handle [1].to_java == [1]
            return asBoolean(context,  equalsRubyArray((RubyArray) other) );
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
        if (obj instanceof ArrayJavaProxy that) {
            equals = arraysEquals(this.getObject(), that.getObject());
        } else if ( obj.getClass().isArray() ) {
            equals = arraysEquals(getObject(), obj);
        }
        return asBoolean(context, equals);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayJavaProxy that) {
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

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCode());
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
        var context = getRuntime().getCurrentContext();

        RubyObject clone = new ArrayJavaProxy(context.runtime, getMetaClass(), cloneObject(), converter);
        clone.setMetaClass(getSingletonClassCloneAndAttach(context, null));

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
        if (args.length == 1) return getRange(context, args[0]);
        if (args.length == 2) return getRange(context, args[0], args[1]);

        throw argumentError(context, args.length, 1);
    }

    public IRubyObject getRange(ThreadContext context, IRubyObject arg0) {
        return arrayRange(context, castAsRange(context, arg0));
    }

    private IRubyObject arrayRange(final ThreadContext context, final RubyRange range) {
        final Object array = getObject();
        final int arrayLength = Array.getLength( array );
        int first = castAsFixnum(context, range.first(context), "Only Integer ranges supported").asInt(context);
        int last = castAsFixnum(context, range.last(context), "Only Integer ranges supported").asInt(context);

        first = first >= 0 ? first : arrayLength + first;
        if (first < 0 || first >= arrayLength) return context.nil;

        last = last >= 0 ? last : arrayLength + last;

        int newLength = last - first;
        if (!range.isExcludeEnd()) newLength++;

        return newLength <= 0 ?
                ArrayUtils.emptyJavaArrayDirect(context, array.getClass().getComponentType()) :
                subarrayProxy(context, array, arrayLength, first, newLength);
    }

    public IRubyObject getRange(ThreadContext context, IRubyObject first, IRubyObject length) {
        return arrayRange(context, first, length);
    }

    private IRubyObject arrayRange(final ThreadContext context,
        final IRubyObject rFirst, final IRubyObject rLength) {
        final Object array = getObject();
        final int arrayLength = Array.getLength( array );

        int first = castAsFixnum(context, rFirst, "Only Integer ranges supported").asInt(context);
        int length = castAsFixnum(context, rLength, "Only Integer ranges supported").asInt(context);

        if (length > arrayLength) throw indexError(context, "length specified is longer than array");
        if (length < 0) return context.nil;

        first = first >= 0 ? first : arrayLength + first;

        if (first >= arrayLength) return context.nil;

        return subarrayProxy(context, array, arrayLength, first, length);
    }

    private IRubyObject subarrayProxy(ThreadContext context, Object ary, final int aryLength, int index, int size) {
        if (index + size > aryLength) size = aryLength - index;

        ArrayJavaProxy proxy = ArrayUtils.newProxiedArray(context, ary.getClass().getComponentType(), converter, size);
        System.arraycopy(ary, index, proxy.getObject(), 0, size);

        return proxy;
    }

    private static final class ArrayNewMethod extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne {

        private final DynamicMethod newMethod;

        ArrayNewMethod(RubyModule implClass, Visibility visibility) {
            this(implClass, visibility, implClass.searchMethod("new"));
        }

        public ArrayNewMethod(RubyModule implClass, Visibility visibility, DynamicMethod oldNew) {
            super(implClass, visibility, "new");
            this.newMethod = oldNew;
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            if (!(arg0 instanceof ArrayJavaProxy)) throw typeError(context, arg0, "ArrayJavaProxy");

            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy");
            proxy.dataWrapStruct(arg0);
            return proxy;
        }

    }

    /**
     * A size method suitable for lambda method reference implementation of {@link RubyEnumerator.SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see RubyEnumerator.SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    protected static IRubyObject size(ThreadContext context, ArrayJavaProxy self, IRubyObject[] args) {
        return self.length(context);
    }
}
