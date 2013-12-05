/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.util.DefinedMessage;

/**
 *
 * @author enebo
 */
public class Colon2ConstNode extends Colon2Node {
    private ConstantCache cache;
    
    public Colon2ConstNode(ISourcePosition position, Node leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyModule target = Helpers.checkIsModule(leftNode.interpret(runtime, context, self, aBlock));
        IRubyObject value = getValue(context, target);

        return value != null ? value : target.getConstantFromConstMissing(name);
    }

    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject lastError = context.getErrorInfo();
        try {
            if (Helpers.isModuleAndHasConstant(
                    leftNode.interpret(runtime, context, self, aBlock), name)) {
                return runtime.getDefinedMessage(DefinedMessage.CONSTANT);
            }
        } catch (JumpException e) {
            // replace lastError
            context.setErrorInfo(lastError);
        }

        return null;
    }

    public IRubyObject getValue(ThreadContext context, RubyModule target) {
        ConstantCache cache = this.cache;

        return ConstantCache.isCachedFrom(target, cache) ? cache.value : reCache(context, target);
    }

    public IRubyObject reCache(ThreadContext context, RubyModule target) {
        Invalidator invalidator = context.runtime.getConstantInvalidator(name);
        Object newGeneration = invalidator.getData();
        IRubyObject value = target.getConstantFromNoConstMissing(name, false);

        if (value != null) {
            cache = new ConstantCache(value, newGeneration, invalidator, target.hashCode());
        } else {
            cache = null;
        }

        return value;
    }
}
