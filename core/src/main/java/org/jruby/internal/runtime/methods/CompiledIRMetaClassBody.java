package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRMetaClassBody extends CompiledIRMethod {
    private final IRMetaClassBody irMetaClassBody;
    private final boolean reuseParentDynScope;
    private final boolean pushNewDynScope;
    private final boolean popDynScope;

    public CompiledIRMetaClassBody(MethodHandle handle, IRScope scope, RubyModule implementationClass) {
        super(handle, scope, Visibility.PUBLIC, implementationClass);

        irMetaClassBody = (IRMetaClassBody)scope;
        this.reuseParentDynScope = scope.getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE);
        this.pushNewDynScope = !scope.getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED) && !this.reuseParentDynScope;
        this.popDynScope = this.pushNewDynScope || this.reuseParentDynScope;
    }

    public String[] getParameterList() {
        return new String[0];
    }

    @Override
    protected void post(ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (popDynScope) {
            context.popScope();
        }
    }

    @Override
    protected void pre(ThreadContext context, StaticScope staticScope, RubyModule implementationClass, IRubyObject self, String name, Block block) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(implementationClass, name, self, block);
        if (pushNewDynScope) {
            // Add a parent-link to current dynscope to support non-local returns cheaply
            // This doesn't affect variable scoping since local variables will all have
            // the right scope depth.
            context.pushScope(DynamicScope.newDynamicScope(staticScope, context.getCurrentScope()));
        }
        context.setCurrentVisibility(getVisibility());
    }

    @Override
    public DynamicMethod dup() {
        CompiledIRMetaClassBody x = new CompiledIRMetaClassBody(variable, method, implementationClass);

        return x;
    }
}
