/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;

/**
 *
 * @author enebo
 */
public class Colon2ConstNode extends Colon2Node {
    private volatile transient IRubyObject cachedValue = null;
    private volatile Object generation = -1;
    private volatile int hash = -1;
    
    public Colon2ConstNode(ISourcePosition position, Node leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyModule target = RuntimeHelpers.checkIsModule(leftNode.interpret(runtime, context, self, aBlock));
        IRubyObject value = getValue(context, target);

        return value != null ? value : target.getConstantFromConstMissing(name);
    }

    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject lastError = context.getErrorInfo();
        try {
            if (RuntimeHelpers.isModuleAndHasConstant(
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
        IRubyObject value = cachedValue; // Store to temp so it does null out on us mid-stream

        return isCached(context, target, value) ? value : reCache(context, target);
    }

    private boolean isCached(ThreadContext context, RubyModule target, IRubyObject value) {
        // We could probably also detect if LHS value came out of cache and avoid some of this
        return
                value != null &&
                generation == context.runtime.getConstantInvalidator().getData() &&
                hash == target.hashCode();
    }

    public IRubyObject reCache(ThreadContext context, RubyModule target) {
        Object newGeneration = context.runtime.getConstantInvalidator().getData();
        IRubyObject value = target.getConstantFromNoConstMissing(name, false);

        cachedValue = value;

        if (value != null) {
            generation = newGeneration;
            hash = target.hashCode();
        }

        return value;
    }
}
