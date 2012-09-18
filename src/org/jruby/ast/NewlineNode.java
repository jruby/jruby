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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
import org.jruby.RubyString;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * A new (logical) source code line.
 * This is used to change the value of the ruby interpreter source and line values.
 * There is one such node for each logical line.  Logical line differs
 * from physical line in that a ';' can be used to make several logical
 * line out of a physical line and a physical line if it is in a comment
 * or in a string does not necessarily correspond to a physical line.
 * This is normally a wrapper around another more significant node.
 * The parser generates such a node around each separate statement.  
 */
public class NewlineNode extends Node {
    private final Node nextNode;

    public NewlineNode(ISourcePosition position, Node nextNode) {
        super(position);

        assert nextNode != null : "nextNode is not null";
        
        this.nextNode = nextNode;
    }

    public NodeType getNodeType() {
        return NodeType.NEWLINENODE;
    }

    /**
     * RubyMethod used by visitors.
     * accepts the visitor
     * @param iVisitor the visitor to accept
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitNewlineNode(this);
    }

    /**
     * Gets the nextNode.
     * @return Returns a Node
     */
    public Node getNextNode() {
        return nextNode;
    }
    
    public List<Node> childNodes() {
        return createList(nextNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        ISourcePosition position = getPosition();
        // something in here is used to build up ruby stack trace...
        context.setLine(position.getLine());

        if (runtime.hasEventHooks()) {
            ASTInterpreter.callTraceFunction(runtime, context, RubyEvent.LINE);
        }

        // TODO: do above but not below for additional newline nodes
        return nextNode.interpret(runtime, context, self, aBlock);
    }

    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return nextNode.definition(runtime, context, self, aBlock);
    }
}
