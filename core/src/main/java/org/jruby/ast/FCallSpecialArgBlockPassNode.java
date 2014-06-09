/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class FCallSpecialArgBlockPassNode extends FCallNode implements SpecialArgs {
    // For 'foo()'.  Args are only significant in maintaining backwards compatible AST structure
    public FCallSpecialArgBlockPassNode(ISourcePosition position, String name, Node args, BlockPassNode iter) {
        super(position, name, args, iter);
    }
}
