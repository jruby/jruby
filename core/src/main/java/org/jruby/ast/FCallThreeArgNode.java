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
public class FCallThreeArgNode extends FCallNode {
    public FCallThreeArgNode(ISourcePosition position, String name, ArrayNode args) {
        super(position, name, args, null);
        
        assert args.size() == 3 : "args.size() is 3";
    }

    @Override
    public Node setIterNode(Node iterNode) {
        return new FCallThreeArgBlockNode(getPosition(), getName(), (ArrayNode) getArgsNode(), (IterNode) iterNode);
    }
}
