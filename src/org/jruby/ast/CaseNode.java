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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Case statement.  Represents a complete case statement, including the body with its
 * when statements.
 */
public class CaseNode extends Node {
	/**
	 * the case expression.
	 **/
    private final Node caseNode;
	/**
	 * A list of all choices including else
	 */
    private final ListNode cases;
    private Node elseNode = null;
    
    public CaseNode(ISourcePosition position, Node caseNode, ListNode cases) {
        super(position);
        
        assert cases != null : "caseBody is not null";
        // TODO: Rewriter and compiler assume case when empty expression.  In MRI this is just
        // a when.
//        assert caseNode != null : "caseNode is not null";
        
        this.caseNode = caseNode;
        this.cases = cases;
    }

    public void setElseNode(Node elseNode) {
        this.elseNode = elseNode;
    }

    public NodeType getNodeType() {
        return NodeType.CASENODE;
    }

 	/**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitCaseNode(this);
    }

    /**
     * Gets the caseNode.
	 * caseNode is the case expression 
     * @return caseNode
     */
    public Node getCaseNode() {
        return caseNode;
    }
    
    public ListNode getCases() {
        return cases;
    }

    public Node getElseNode() {
        return elseNode;
    }

    /**
     * Gets the first whenNode.
	 * the body of the case statement, the first of a list of WhenNodes
     * @return whenNode
     */
    @Deprecated
    public Node getFirstWhenNode() {
        return cases;
    }
    
    public List<Node> childNodes() {
        return Node.createList(caseNode, cases);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject expression = caseNode == null ? null : caseNode.interpret(runtime, context, self, aBlock);

        context.pollThreadEvents();

        for (Node child : cases.childNodes()) {
            WhenNode when = (WhenNode) child;
            ISourcePosition position = child.getPosition();

            context.setFileAndLine(position.getFile(), position.getStartLine());

            if (runtime.hasEventHooks()) ASTInterpreter.callTraceFunction(runtime, context, RubyEvent.LINE);
            IRubyObject result = when.when(expression, context, runtime, self, aBlock);
            if (result != null) return result;
            context.pollThreadEvents();
        }

        return elseNode != null ? elseNode.interpret(runtime, context, self, aBlock) : runtime.getNil();
    }
}
