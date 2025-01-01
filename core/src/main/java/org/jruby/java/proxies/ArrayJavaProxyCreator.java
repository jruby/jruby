package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Define.defineClass;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

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

    final Class<?> elementType;
    int[] dimensions = EMPTY;

    public static RubyClass createArrayJavaProxyCreator(ThreadContext context, RubyClass Object) {
        return defineClass(context, "ArrayJavaProxyCreator", Object, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, ArrayJavaProxyCreator.class);
    }

    ArrayJavaProxyCreator(final ThreadContext context, Class<?> elementType, final IRubyObject[] sizes) {
        this(context.runtime, elementType);
        assert sizes.length > 0;
        aggregateDimensions(context, sizes);
    }

    private ArrayJavaProxyCreator(final Ruby runtime, Class<?> elementType) {
        super(runtime, runtime.getJavaSupport().getArrayJavaProxyCreatorClass());
        this.elementType = elementType;
    }

    @JRubyMethod(name = "[]", required = 1, rest = true, checkArity = false)
    public final IRubyObject op_aref(ThreadContext context, IRubyObject[] sizes) {
        Arity.checkArgumentCount(context, sizes, 1, -1);
        aggregateDimensions(context, sizes);
        return this;
    }

    @JRubyMethod(name = { "new", "new_instance" })
    public final ArrayJavaProxy new_instance(ThreadContext context) {
        return ArrayJavaProxy.newArray(context.runtime, elementType, dimensions);
    }

    private void aggregateDimensions(ThreadContext context, final IRubyObject[] sizes) {
        final int slen = sizes.length; if ( slen == 0 ) return;
        final int dlen = dimensions.length;
        final int[] newDimensions = new int[ dlen + slen ];
        if ( dlen == 1 ) newDimensions[0] = dimensions[0];
        else {
            System.arraycopy(dimensions, 0, newDimensions, 0, dlen);
        }
        for ( int i = 0; i < slen; i++ ) {
            int size = toInt(context, sizes[i]);
            newDimensions[ i + dlen ] = size;
        }
        dimensions = newDimensions;
    }
}
