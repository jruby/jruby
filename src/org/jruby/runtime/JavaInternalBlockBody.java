/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a special Java implementation of a block.
 */
public abstract class JavaInternalBlockBody extends BlockBody {
    private final Arity arity;
    private final ThreadContext originalContext;
    private final String methodName;
    
    /**
     * For blocks which can be executed in any thread concurrently.
     */
    public JavaInternalBlockBody(Arity arity) {
        this(null, null, arity);
    }

    /**
     * For blocks which cannot be executed in parallel.
     * @param methodName
     * @param arity 
     */
    public JavaInternalBlockBody(ThreadContext originalContext, String methodName, Arity arity) {
        super(BlockBody.SINGLE_RESTARG);
        
        this.arity = arity;
        this.originalContext = originalContext;
        this.methodName = methodName;
    }
    
    // Make sure we are still on the same thread as originator if we care
    private void threadCheck(ThreadContext yieldingContext) {
        if (originalContext != null && yieldingContext != originalContext) 
            throw yieldingContext.getRuntime().newThreadError("" + methodName + " cannot be parallelized");        
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        threadCheck(context);
        
        return yield(context, value);        
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Type type) {
        threadCheck(context);
        
        
        return yield(context, value);
    }
    
    public abstract IRubyObject yield(ThreadContext context, IRubyObject value);

    @Override
    public StaticScope getStaticScope() {
        throw new RuntimeException("CallBlock does not have a static scope; this should not be called");
    }

    @Override
    public void setStaticScope(StaticScope newScope) {
        throw new RuntimeException("CallBlock does not have a static scope; this should not be called");
    }

    @Override
    public Block cloneBlock(Binding binding) {
        return new Block(this, binding);
    }

    @Override
    public Arity arity() {
        return arity;
    }

    @Override
    public String getFile() {
        return "(internal)";
    }

    @Override
    public int getLine() {
        return -1;
    }
    
}
