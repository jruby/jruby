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
    public Colon2ConstNode(ISourcePosition position, Node leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return RuntimeHelpers.checkIsModule(leftNode.interpret(runtime, context, self, aBlock)).fastGetConstantFrom(name);
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
}
