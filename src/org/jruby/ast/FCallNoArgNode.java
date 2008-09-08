/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class FCallNoArgNode extends FCallNode {
    // For 'foo'
    public FCallNoArgNode(ISourcePosition position, String name) {
        super(position, name, null, null);
    }

    // For 'foo()'.  Args are only significant in maintaining backwards compatible AST structure
    public FCallNoArgNode(ISourcePosition position, Node args, String name) {
        super(position, name, args, null);
    }
        
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return callAdapter.call(context, self, self);
    }
    
    @Override
    public Node setIterNode(Node iterNode) {
        return new FCallNoArgBlockNode(getPosition(), getName(), getArgsNode(), (IterNode) iterNode);
    }
}
