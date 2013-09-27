package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

/**
 * Operands extending this type can make a reasonable assumption of
 * immutability.  In Ruby, almost nothing is truly immutable (set_instance_var)
 * but for the sake of our compiler we can assume the basic behavior will
 * continue to work unchanged (the value of an instance of fixnum 3 will remain
 * 3).
 *
 * Knowing that we have a literal which will not change can be used for
 * optimizations like constant propagation.
 *
 * ENEBO: This cachedObject thing obviously cannot be used from multiple
 * threads without some safety being added.  This probably also is not the
 * fastest code, but it is very simple.  It would be really nice to make this
 * side-effect free as well, but this is difficult without adding a level of
 * indirection or pre-caching each value we encounter during construction.
 */
public abstract class ImmutableLiteral extends Operand {
    private Object cachedObject = null;

    @Override
    public boolean hasKnownValue() {
        return true;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }

    /**
     * Implementing class is responsible for constructing the cached value.
     */
    public abstract Object createCacheObject(ThreadContext context);


    /**
     * Returns the cached object.  If not then it asks class to create an
     * object to cache.
     */
    public Object cachedObject(ThreadContext context) {
        if (!isCached()) cachedObject = createCacheObject(context);

        return cachedObject;
    }

    /**
     * Has this object already been cached?
     */
    public boolean isCached() {
        return cachedObject != null;
    }

    /**
     * retrieve the live value represented by this immutable literal.  An
     * interesting property of knowing something cannot change at compile
     * time is that all information neccesary to construct it is also known
     * at compile time.  We don't pre-create these since we don't want to
     * assume the cost of constructing literals which may never be used.
     */
    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return cachedObject(context);
    }
}
