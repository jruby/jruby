/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class MethodMissingMethod extends DynamicMethod {
    private RubyString name;

    public MethodMissingMethod(RubyModule implementationClass, RubyString name) {
        super(implementationClass, null, null);

        this.name = name;
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        args = createArgs(args);

        // TODO: Callsite cache of method_missing
        return self.callMethod(context, "method_missing", args, block);
    }

    private IRubyObject[] createArgs(IRubyObject[] args) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];

        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = name;

        return newArgs;
    }

    public DynamicMethod dup() {
        return new MethodMissingMethod(getImplementationClass(), name);
    }
}
