package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.ir.JIT;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @see org.jruby.java.codegen.BlockInterfaceGenerator
 */
public abstract class BlockInterfaceTemplate {
    public final Ruby runtime;
    private final Block block;

    public BlockInterfaceTemplate(final RubyProc proc) {
        assert proc != null;
        this.runtime = proc.getRuntime();
        this.block = proc.getBlock();
    }

    @JIT
    @SuppressWarnings("unused")
    protected final IRubyObject __ruby_call(final Class<?> returnType) {
        return block.call(runtime.getCurrentContext());
    }

    @JIT
    @SuppressWarnings("unused")
    protected final IRubyObject __ruby_call(final Class<?> returnType, IRubyObject arg0) {
        return block.call(runtime.getCurrentContext(), arg0);
    }

    @JIT
    @SuppressWarnings("unused")
    protected final IRubyObject __ruby_call(final Class<?> returnType, IRubyObject arg0, IRubyObject arg1) {
        return block.call(runtime.getCurrentContext(), arg0, arg1);
    }

    @JIT
    @SuppressWarnings("unused")
    protected final IRubyObject __ruby_call(final Class<?> returnType, IRubyObject[] args) {
        return block.call(runtime.getCurrentContext(), args);
    }
}
