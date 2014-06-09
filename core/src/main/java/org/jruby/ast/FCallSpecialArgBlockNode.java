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
 * For SplatNode and ArgsCatNode calls.
 */
public class FCallSpecialArgBlockNode extends FCallNode implements SpecialArgs {
    public FCallSpecialArgBlockNode(ISourcePosition position, String name, Node args, IterNode iter) {
        super(position, name, args, iter);
    }
}
