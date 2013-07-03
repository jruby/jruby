/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Range in a boolean expression (named after a FlipFlop component in electronic?).
 */
public class FlipNode extends Node {
    private final Node beginNode;
    private final Node endNode;
    private final boolean exclusive;
    // A scoped location of this variable (high 16 bits is how many scopes down and low 16 bits
    // is what index in the right scope to set the value.
    private final int location;
    
    public FlipNode(ISourcePosition position, Node beginNode, Node endNode, boolean exclusive, int location) {
        super(position);
        
        assert beginNode != null : "beginNode is not null";
        assert endNode != null : "endNode is not null";
        
        this.beginNode = beginNode;
        this.endNode = endNode;
        this.exclusive = exclusive;
        this.location = location;
    }

    public NodeType getNodeType() {
        return NodeType.FLIPNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitFlipNode(this);
    }

    /**
     * Gets the beginNode.
	 * beginNode will set the FlipFlop the first time it is true
     * @return Returns a Node
     */
    public Node getBeginNode() {
        return beginNode;
    }

    /**
     * Gets the endNode.
	 * endNode will reset the FlipFlop when it is true while the FlipFlop is set.
     * @return Returns a Node
     */
    public Node getEndNode() {
        return endNode;
    }

    /**
     * Gets the exclusive.
	 * if the range is a 2 dot range it is false if it is a three dot it is true
     * @return Returns a boolean
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * How many scopes should we burrow down to until we need to set the block variable value.
     * 
     * @return 0 for current scope, 1 for one down, ...
     */
    public int getDepth() {
        return location >> 16;
    }
    
    /**
     * Gets the index within the scope construct that actually holds the eval'd value
     * of this local variable
     * 
     * @return Returns an int offset into storage structure
     */
    public int getIndex() {
        return location & 0xffff;
    }
    
    public List<Node> childNodes() {
        return Node.createList(beginNode, endNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        DynamicScope scope = context.getCurrentScope();
        IRubyObject nil = runtime.getNil();
        int index = getIndex();

        // Make sure the appropriate scope has proper size. See JRUBY-2046.
        DynamicScope flipScope = scope.getFlipScope();
        flipScope.growIfNeeded();
        
        IRubyObject result = flipScope.getValueDepthZeroOrNil(index, nil);
   
        if (exclusive) {
            if (result == null || !result.isTrue()) {
                result = trueIfTrue(runtime, beginNode.interpret(runtime, context, self, aBlock));
                flipScope.setValueDepthZero(result, index);
                return result;
            } else {
                if (endNode.interpret(runtime, context, self, aBlock).isTrue()) {
                    flipScope.setValueDepthZero(runtime.getFalse(), index);
                }
                
                return runtime.getTrue();
            }
        } else {
            if (result == null || !result.isTrue()) {
                if (beginNode.interpret(runtime, context, self, aBlock).isTrue()) {
                    flipScope.setValueDepthZero(falseIfTrue(runtime, endNode.interpret(runtime, context, self, aBlock)), index);
                    return runtime.getTrue();
                } 

                return runtime.getFalse();
            } else {
                if (endNode.interpret(runtime, context, self, aBlock).isTrue()) {
                    flipScope.setValueDepthZero(runtime.getFalse(), index);
                }
                return runtime.getTrue();
            }
        }
    }
    
    private static RubyBoolean trueIfTrue(Ruby runtime, IRubyObject truish) {
        return truish.isTrue() ? runtime.getTrue() : runtime.getFalse();
    }
    
    private static RubyBoolean falseIfTrue(Ruby runtime, IRubyObject truish) {
        return truish.isTrue() ? runtime.getFalse() : runtime.getTrue();
    }
}
