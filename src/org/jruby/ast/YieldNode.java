/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * Represents a yield statement.
 */
public class YieldNode extends Node {
    private final Node argsNode;
    private final boolean expandedArguments;

    /**
     * Construct a new YieldNode.
     *
     * @param position position of the node in the source
     * @param argsNode the arguments to the yield
     * @param expandedArguments whether the arguments should be treated as directly-passed args
     *                          as in yield 1, 2, 3 (expandArguments = true) versus
     *                          yield [1, 2, 3] (expandArguments = false).
     */
    public YieldNode(ISourcePosition position, Node argsNode, boolean expandedArguments) {
        super(position);
        
        // block.yield depends on null to represent empty and nil to represent nil - [nil] vs []
        //assert argsNode != null : "argsNode is not null";
        
        this.argsNode = argsNode;
        // If we have more than one argument, then make sure the array is not ObjectSpaced.
        if (argsNode instanceof ArrayNode) {
            ((ArrayNode)argsNode).setLightweight(true);
        }
        this.expandedArguments = expandedArguments;
    }

    public NodeType getNodeType() {
        return NodeType.YIELDNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitYieldNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a Node
     */
    public Node getArgsNode() {
        return argsNode;
    }

    @Deprecated
    public boolean getCheckState() {
        return expandedArguments;
    }

    public boolean getExpandArguments() {
        return expandedArguments;
    }

    public List<Node> childNodes() {
        return createList(argsNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        if (expandedArguments) {
            return context.getCurrentFrame().getBlock().yield(context, argsNode.interpret(runtime, context, self, aBlock), null, null, true);
        } 

        return context.getCurrentFrame().getBlock().yield(context, argsNode.interpret(runtime, context, self, aBlock));
    }
    
    @Override
    public String definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return aBlock.isGiven() ? "yield" : null;
    }
}
