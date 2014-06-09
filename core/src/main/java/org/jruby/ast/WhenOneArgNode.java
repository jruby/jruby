/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.ast.types.IEqlNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class WhenOneArgNode extends WhenNode {
    public WhenOneArgNode(ISourcePosition position, Node expressionNode, Node bodyNode, Node nextCase) {
        super(position, expressionNode, bodyNode, nextCase);
    }
}
