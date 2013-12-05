package org.jruby.java.proxies;

import java.lang.reflect.Array;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A shim class created when constructing primitive arrays from proxied Java classes.
 *
 * In the following code:
 *
 * <code>Java::byte[50].new</code>
 *
 * The [50] call produces an instance of ArrayJavaProxyCreator. The [] method can
 * be called with any number of integer arguments to create a multi-dimensional
 * array creator, or calls to [] can be chained for the same effect. The eventual
 * call to new is against the ArrayJavaProxyCreator instance, and the result is
 * a wrapped Java array of the specified dimensions and element type of the original
 * receiver (the byte class, above).
 */
public class ArrayJavaProxyCreator extends RubyObject {
    private static final int[] EMPTY = new int[0];
    Class elementClass;
    int[] dimensions = EMPTY;

    public static RubyClass createArrayJavaProxyCreator(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyClass arrayJavaProxyCreator = runtime.defineClass("ArrayJavaProxyCreator", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        arrayJavaProxyCreator.defineAnnotatedMethods(ArrayJavaProxyCreator.class);
        return arrayJavaProxyCreator;
    }

    public ArrayJavaProxyCreator(Ruby runtime) {
        super(runtime, runtime.getJavaSupport().getArrayJavaProxyCreatorClass());
    }

    public void setup(ThreadContext context, IRubyObject javaClass, IRubyObject[] sizes) {
        elementClass = (Class) javaClass.toJava(Class.class);
        aggregateDimensions(sizes);
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject op_aref(ThreadContext context, IRubyObject[] sizes) {
        Arity.checkArgumentCount(context.runtime, sizes, 1, -1);
        aggregateDimensions(sizes);
        return this;
    }

    @JRubyMethod(name = "new")
    public IRubyObject _new(ThreadContext context) {
        Ruby runtime = context.runtime;
        Object array = Array.newInstance(elementClass, dimensions);
        
        return new ArrayJavaProxy(runtime, Java.getProxyClassForObject(runtime, array), array);
    }

    private void aggregateDimensions(IRubyObject[] sizes) {
        int[] newDimensions = new int[dimensions.length + sizes.length];
        System.arraycopy(dimensions, 0, newDimensions, 0, dimensions.length);
        for (int i = 0; i < sizes.length; i++) {
            IRubyObject size = sizes[i];
            int intSize = (int) size.convertToInteger().getLongValue();
            newDimensions[i + dimensions.length] = intSize;
        }
        dimensions = newDimensions;
    }
}
