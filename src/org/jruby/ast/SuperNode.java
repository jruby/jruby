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
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.DefinedMessage;

/**
 * A call to super(...) with arguments to a method.
 */
public class SuperNode extends Node implements BlockAcceptingNode {
    private final Node argsNode;
    private Node iterNode;
    private final CallSite callSite;

    public SuperNode(ISourcePosition position, Node argsNode) {
        this(position, argsNode, null);
    }
    
    public SuperNode(ISourcePosition position, Node argsNode, Node iterNode) {
        super(position);
        this.argsNode = argsNode;
        this.iterNode = iterNode;
        if (argsNode instanceof ArrayNode) {
            ((ArrayNode)argsNode).setLightweight(true);
        }
        this.callSite = MethodIndex.getSuperCallSite();
    }

    public NodeType getNodeType() {
        return NodeType.SUPERNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitSuperNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a Node
     */
    public Node getArgsNode() {
        return argsNode;
    }
    
    public List<Node> childNodes() {
        return iterNode != null ? createList(argsNode, iterNode) : createList(argsNode); 
    }

    public Node getIterNode() {
        return iterNode;
    }

    public Node setIterNode(Node iterNode) {
        this.iterNode = iterNode;
        
        return this;
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject[] args = ASTInterpreter.setupArgs(runtime, context, argsNode, self, aBlock);
        Block block = ASTInterpreter.getBlock(runtime, context, self, aBlock, iterNode);
        
        // If no explicit block passed to super, then use the one passed in, unless it's explicitly cleared with nil
        if (iterNode == null && !block.isGiven()) block = aBlock;

        // dispatch as varargs, so incoming args are used to decide arity path
        return callSite.callVarargs(context, self, self, args, block);
    }
    
    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        String name = context.getFrameName();
        RubyModule klazz = context.getFrameKlazz();

        if (name != null &&
                klazz != null &&
                Helpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass().isMethodBound(name, false)) {
            return ASTInterpreter.getArgumentDefinition(runtime, context, argsNode, runtime.getDefinedMessage(DefinedMessage.SUPER), self, aBlock);
        }
            
        return null;
    }
}
