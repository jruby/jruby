/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class Colon2ConstNode extends Colon2Node {
    private volatile transient IRubyObject cachedValue = null;
    private volatile int generation = -1;
    private volatile int hash = -1;
    
    public Colon2ConstNode(ISourcePosition position, Node leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyModule target = RuntimeHelpers.checkIsModule(leftNode.interpret(runtime, context, self, aBlock));
        IRubyObject value = getValue(context, target);

        return value != null ? value : target.fastGetConstantFromConstMissing(name);
    }

    @Override
    public String definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
       try {
            if (isModuleAndHasConstant(leftNode.interpret(runtime, context, self, aBlock))) return "constant";
        } catch (JumpException e) {}

        return null;
    }

    private boolean isModuleAndHasConstant(IRubyObject left) {
        return left instanceof RubyModule && ((RubyModule) left).fastGetConstantAt(name) != null;
    }

    public IRubyObject getValue(ThreadContext context, RubyModule target) {
        IRubyObject value = cachedValue; // Store to temp so it does null out on us mid-stream

        return isCached(context, target, value) ? value : reCache(context, target);
    }

    private boolean isCached(ThreadContext context, RubyModule target, IRubyObject value) {
        // We could probably also detect if LHS value came out of cache and avoid some of this
        return
                value != null &&
                generation == context.getRuntime().getConstantGeneration() &&
                hash == target.hashCode();
    }

    public IRubyObject reCache(ThreadContext context, RubyModule target) {
        int newGeneration = context.getRuntime().getConstantGeneration();
        IRubyObject value = target.fastGetConstantFromNoConstMissing(name);

        cachedValue = value;

        if (value != null) {
            generation = newGeneration;
            hash = target.hashCode();
        }

        return value;
    }
}
