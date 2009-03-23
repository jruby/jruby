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
import org.jruby.RubyModule;
import org.jruby.ast.types.IArityNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * a call to 'super' with no arguments in a method.
 */
public class ZSuperNode extends Node implements IArityNode, BlockAcceptingNode {
    private Node iterNode;
    private CallSite callSite;

    public ZSuperNode(ISourcePosition position) {
        super(position);
        this.callSite = MethodIndex.getSuperCallSite();
    }

    public NodeType getNodeType() {
        return NodeType.ZSUPERNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitZSuperNode(this);
    }
	
    /**
     * 'super' can take any number of arguments.
     */
    public Arity getArity() {
        return Arity.optional();
    }
    
    public List<Node> childNodes() {
        return iterNode != null ? createList(iterNode) : EMPTY_LIST;
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
        Block block = ASTInterpreter.getBlock(runtime, context, self, aBlock, iterNode);
        if (block == null || !block.isGiven()) block = context.getFrameBlock();

        return callSite.call(context, self, self, context.getCurrentScope().getArgValues(), block);
    }
    
    @Override
    public String definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        String name = context.getFrameName();
        RubyModule klazz = context.getFrameKlazz();
        
        if (name != null && klazz != null && klazz.getSuperClass().isMethodBound(name, false)) {
            return "super";
        }
        
        return null;
    }
}
