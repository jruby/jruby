package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Supports optimization for define_method.  block that defined_method sees happens to be the block
 * passed to the scope where the define_method originally was called...not the block potentially passed to
 * the newly defined method.
 */
public class DefineMethodMethod extends MixedModeIRMethod {

    private final Block capturedBlock;

    public DefineMethodMethod(IRScope method, Visibility visibility, RubyModule implementationClass, Block capturedBlock) {
        super(method, visibility, implementationClass);

        this.capturedBlock = capturedBlock;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        return super.call(context, self, clazz, name, args, capturedBlock);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        return super.call(context, self, clazz, name, capturedBlock);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        return super.call(context, self, clazz, name, arg0, capturedBlock);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return super.call(context, self, clazz, name, arg0, arg1, capturedBlock);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return super.call(context, self, clazz, name, arg0, arg1, arg2, capturedBlock);
    }
}
