package org.jruby.ext.fiber;

import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Fiber")
public abstract class Fiber extends RubyObject implements ExecutionContext {
    private final Map<Object, IRubyObject> contextVariables = new WeakHashMap<Object, IRubyObject>();
    protected Block block;
    protected RubyThread parent;
    protected boolean root;
    private Fiber transferredFrom;
    private Fiber transferredTo;

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        final Ruby runtime = context.getRuntime();
        if (!root && (block == null || !block.isGiven())) {
            throw runtime.newArgumentError("tried to create Proc object without a block");
        }
        this.block = block;
        this.parent = context.getThread();
        
        initFiber(context);
        
        return this;
    }

    public Fiber(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    protected abstract void initFiber(ThreadContext context);

    protected abstract IRubyObject resumeOrTransfer(ThreadContext context, IRubyObject arg, boolean transfer);

    public abstract IRubyObject yield(ThreadContext context, IRubyObject res);
    
    public abstract boolean isAlive();

    public boolean isRoot() {
        return root;
    }

    public Fiber makeRootFiber() {
        root = true;
        return this;
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject resume(ThreadContext context) {
        return resumeOrTransfer(context, context.nil, false);
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject resume(ThreadContext context, IRubyObject arg) {
        return resumeOrTransfer(context, arg, false);
    }

    @JRubyMethod(rest = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject resume(ThreadContext context, IRubyObject[] args) {
        return resumeOrTransfer(context, context.getRuntime().newArrayNoCopyLight(args), false);
    }

    // This should only be defined after require 'fiber'
    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject transfer(ThreadContext context) {
        return resumeOrTransfer(context, context.nil, true);
    }

    // This should only be defined after require 'fiber'
    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject transfer(ThreadContext context, IRubyObject arg) {
        return resumeOrTransfer(context, arg, true);
    }

    // This should only be defined after require 'fiber'
    @JRubyMethod(rest = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject transfer(ThreadContext context, IRubyObject[] args) {
        return resumeOrTransfer(context, context.getRuntime().newArrayNoCopyLight(args), true);
    }

    public Map<Object, IRubyObject> getContextVariables() {
        return contextVariables;
    }

    /**
     * @return the transferredFrom
     */
    public Fiber getTransferredFrom() {
        return transferredFrom;
    }

    /**
     * @param transferredFrom the transferredFrom to set
     */
    public void setTransferredFrom(Fiber transferredFrom) {
        this.transferredFrom = transferredFrom;
    }

    /**
     * @return the transferredTo
     */
    public Fiber getTransferredTo() {
        return transferredTo;
    }

    /**
     * @param transferredTo the transferredTo to set
     */
    public void setTransferredTo(Fiber transferredTo) {
        this.transferredTo = transferredTo;
    }
    
}
