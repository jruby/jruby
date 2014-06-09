/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class FCallThreeArgBlockPassNode extends FCallNode {
    
    public FCallThreeArgBlockPassNode(ISourcePosition position, String name, ArrayNode args, BlockPassNode iter) {
        super(position, name, args, iter);
        
        assert args.size() == 3 : "args.size() is 3";
    }

}
