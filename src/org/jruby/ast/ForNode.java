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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.SharedScopeBlock;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A 'for' statement.  This is implemented using iter and that is how MRI does things,
 * but 'for's do not have their own stack, so doing this way is mildly painful.
 * 
 * @see IterNode
 */
public class ForNode extends IterNode {
    public final CallSite callAdapter = MethodIndex.getCallSite("each");

    private Node iterNode;

    public ForNode(ISourcePosition position, Node varNode, Node bodyNode, Node iterNode) {
        // For nodes do not have their own scope so we pass null to indicate this.
        // 'For's are implemented as blocks in evaluation, but they have no scope so we
        // just deal with this lack of scope throughout its lifespan.  We should probably
        // change the way this works to get rid of multiple null checks.
        super(position, varNode, null, bodyNode);
        
        assert iterNode != null : "iterNode is not null";
        
        this.iterNode = iterNode;
    }

    public NodeType getNodeType() {
        return NodeType.FORNODE;
    }
    
    public Node getIterNode() {
        return iterNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    @Override
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitForNode(this);
    }
    
    @Override
    public List<Node> childNodes() {
        return Node.createList(getVarNode(), getBodyNode(), iterNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        Block block = SharedScopeBlock.newInterpretedSharedScopeClosure(context, this, context.getCurrentScope(), self);
   
        try {
            while (true) {
                try {
                    String savedFile = context.getFile();
                    int savedLine = context.getLine();
   
                    IRubyObject recv = null;
                    try {
                        recv = iterNode.interpret(runtime, context, self, aBlock);
                    } finally {
                        context.setFileAndLine(savedFile, savedLine);
                    }
   
                    return callAdapter.call(context, self, recv, block);
                } catch (JumpException.RetryJump rj) {
                    // do nothing, allow loop to retry
                }
            }
        } catch (JumpException.BreakJump bj) {
            return (IRubyObject) bj.getValue();
        }
    }
}
