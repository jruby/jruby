/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.DefinedMessage;

/**
 * Represents a constant path which ends in a method (e.g. Foo::bar).  Note: methods with
 * explicit parameters (e.g. Foo::bar()) will be a CallNode.
 */
public class Colon2MethodNode extends Colon2Node {
    public Colon2MethodNode(ISourcePosition position, Node leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "class fooBar is not valid";
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return Helpers.invoke(context, leftNode.interpret(runtime, context, self, aBlock), name, aBlock);
    }

    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
       try {
           if (hasMethod(leftNode.interpret(runtime, context, self, aBlock))) {
               return runtime.getDefinedMessage(DefinedMessage.METHOD);
           }
        } catch (JumpException e) {
        }

        return null;
    }

    private boolean hasMethod(IRubyObject left) {
        return left.getMetaClass().isMethodBound(name, true);
    }
}
