/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * For SplatNode and ArgsCatNode calls.
 */
public class FCallSpecialArgNode extends FCallNode implements SpecialArgs {
    public FCallSpecialArgNode(ISourcePosition position, String name, Node args) {
        super(position, name, args, null);
    }

    @Override
    public Node setIterNode(Node iterNode) {
        return new FCallSpecialArgBlockNode(getPosition(), getName(), getArgsNode(), (IterNode) iterNode);
    }
}
