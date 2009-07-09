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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.RubyArray;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MultipleAsgnNode extends AssignableNode {
    private final ListNode pre;
    private final Node rest;
    
    public MultipleAsgnNode(ISourcePosition position, ListNode pre, Node rest) {
        super(position);
        this.pre = pre;
        this.rest = rest;
    }

    public NodeType getNodeType() {
        return NodeType.MULTIPLEASGNNODE;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitMultipleAsgnNode(this);
    }

    /**
     * Gets the headNode.
     * @return Returns a ListNode
     */
    public ListNode getHeadNode() {
        return pre;
    }

    public ListNode getPre() {
        return pre;
    }

    public int getPreCount() {
        return pre == null ? 0 : pre.size();
    }
    
    /**
     * Gets the argsNode.
     * @return Returns a INode
     */
    public Node getArgsNode() {
        return rest;
    }

    public Node getRest() {
        return rest;
    }
    
    /**
     * Number of arguments is dependent on headNodes size
     */
    @Override
    public Arity getArity() {
        if (rest != null) {
            return Arity.required(pre == null ? 0 : pre.size());
        }
        
        return Arity.fixed(pre.size());
    }
    
    public List<Node> childNodes() {
        return Node.createList(pre, rest, getValueNode());
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        switch (getValueNode().getNodeType()) {
        case ARRAYNODE: {
            ArrayNode iVisited2 = (ArrayNode) getValueNode();
            return ASTInterpreter.multipleAsgnArrayNode(runtime, context, this, iVisited2, self, aBlock);
        }
        case SPLATNODE: {
            SplatNode splatNode = (SplatNode) getValueNode();
            RubyArray rubyArray = RuntimeHelpers.splatValue(splatNode.getValue().interpret(runtime, context, self, aBlock));
            return AssignmentVisitor.multiAssign(runtime, context, self, this, rubyArray, false);
        }
        default:
            IRubyObject value = getValueNode().interpret(runtime, context, self, aBlock);

            if (!(value instanceof RubyArray)) {
                value = RubyArray.newArray(runtime, value);
            }
            
            return AssignmentVisitor.multiAssign(runtime, context, self, this, (RubyArray)value, false);
        }
    }
    
    @Override
    public IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {
        if (!(value instanceof RubyArray)) {
            value = ArgsUtil.convertToRubyArray(runtime, value, pre != null);
        }
        
        return AssignmentVisitor.multiAssign(runtime, context, self, this, (RubyArray) value, checkArity);
    }
}
