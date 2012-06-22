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
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class OpAsgnOrNode extends Node implements BinaryOperatorNode {
    private final Node firstNode;
    private final Node secondNode;

    public OpAsgnOrNode(ISourcePosition position, Node headNode, Node valueNode) {
        super(position);
        
        assert headNode != null : "headNode is not null";
        assert valueNode != null : "valueNode is not null";
        
        firstNode = headNode;
        secondNode = valueNode;
    }

    public NodeType getNodeType() {
        return NodeType.OPASGNORNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitOpAsgnOrNode(this);
    }

    /**
     * Gets the firstNode.
     * @return Returns a Node
     */
    public Node getFirstNode() {
        return firstNode;
    }

    /**
     * Gets the secondNode.
     * @return Returns a Node
     */
    public Node getSecondNode() {
        return secondNode;
    }
    
    public List<Node> childNodes() {
        return Node.createList(firstNode, secondNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject result = runtime.getNil();

        if (defined(runtime, context, firstNode, self, aBlock)) {
            result = firstNode.interpret(runtime, context, self, aBlock);
        }
        if (!result.isTrue()) {
            result = secondNode.interpret(runtime,context, self, aBlock);
        }
   
        return ASTInterpreter.pollAndReturn(context, result);
    }

    private boolean defined(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        return node.definition(runtime, context, self, aBlock) != null;
    }

    @Override
    public ByteList definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        try {
            interpret(runtime, context, self, aBlock);
            return ASSIGNMENT_BYTELIST;
        } catch (JumpException jumpExcptn) {
        }

        return null;
    }
}
