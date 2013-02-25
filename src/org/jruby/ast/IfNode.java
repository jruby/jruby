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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * an 'if' statement.
 */
public class IfNode extends Node {
    private final Node condition;
    private final Node thenBody;
    private final Node elseBody;

    public IfNode(ISourcePosition position, Node condition, Node thenBody, Node elseBody) {
        super(position);
        
        assert condition != null : "condition is not null";
//        assert thenBody != null : "thenBody is not null";
//        assert elseBody != null : "elseBody is not null";
        
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public NodeType getNodeType() {
        return NodeType.IFNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitIfNode(this);
    }

    /**
     * Gets the condition.
     * @return Returns a Node
     */
    public Node getCondition() {
        return condition;
    }

    /**
     * Gets the elseBody.
     * @return Returns a Node
     */
    public Node getElseBody() {
        return elseBody;
    }

    /**
     * Gets the thenBody.
     * @return Returns a Node
     */
    public Node getThenBody() {
        return thenBody;
    }
    
    public List<Node> childNodes() {
        return Node.createList(condition, thenBody, elseBody);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        ISourcePosition position = getPosition();

        context.setFileAndLine(position.getFile(), position.getStartLine());

        IRubyObject result = condition.interpret(runtime, context, self, aBlock);
        
        // TODO: put these nil guards into tree (bigger than I want to do right now)

        // FIXME: JRUBY-3188 ends up with condition returning null...quick fix until I can dig into it
        if (result.isTrue()) {
            return thenBody == null ? runtime.getNil() : thenBody.interpret(runtime, context, self, aBlock);
        } else {
            return elseBody == null ? runtime.getNil() : elseBody.interpret(runtime, context, self, aBlock);
        }
    }
}
