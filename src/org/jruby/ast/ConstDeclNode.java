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
import org.jruby.RubyModule;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Declaration (and assignment) of a Constant.
 */
// FIXME: ConstDecl could be two seperate classes (or done differently since constNode and name
// never exist at the same time.
public class ConstDeclNode extends AssignableNode implements INameNode {
    private final String name;
    private final INameNode constNode;

    // TODO: Split this into two sub-classes so that name and constNode can be specified seperately.
    public ConstDeclNode(ISourcePosition position, String name, INameNode constNode, Node valueNode) {
        super(position, valueNode);
        
        this.name = name;        
        this.constNode = constNode;
    }

    public NodeType getNodeType() {
        return NodeType.CONSTDECLNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitConstDeclNode(this);
    }

    /**
     * Gets the name (this is the rightmost element of lhs (in Foo::BAR it is BAR).
	 * name is the constant Name, it normally starts with a Capital
     * @return name
     */
    public String getName() {
    	return (name == null ? constNode.getName() : name);
    }
    
    /**
     * Get the path the name is associated with or null (in Foo::BAR it is Foo).
     * @return pathNode
     */
    public Node getConstNode() {
        return (Node) constNode;
    }
    
    public List<Node> childNodes() {
        return createList(getValueNode());
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject result = getValueNode().interpret(runtime, context, self, aBlock);
        
        if (constNode == null) {
            return context.setConstantInCurrent(name, result);
        } else if (((Node)constNode).getNodeType() == NodeType.COLON2NODE) {
            Node leftNode = ((Colon2Node) constNode).getLeftNode();
            
            assert leftNode != null : "leftNode is not null";
            
            IRubyObject obj = leftNode.interpret(runtime, context, self, aBlock);
            
            return context.setConstantInModule(constNode.getName(), obj, result);
        } else { // colon3
            return context.setConstantInObject(constNode.getName(), result);
        }
    }

    @Override
    public IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {
        IRubyObject module;

        if (constNode == null) {
            module = context.getCurrentScope().getStaticScope().getModule();
            
            if (module == null) {
                // TODO: wire into new exception handling mechanism
                throw runtime.newTypeError("no class/module to define constant");
            }
        } else if (constNode instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) constNode).getLeftNode();

            if (leftNode == null) {
                module = runtime.getNil();
            } else {
                module = leftNode.interpret(runtime, context, self, block);
            }
        } else { // Colon3
            module = runtime.getObject();
        }

        ((RubyModule) module).fastSetConstant(getName(), value);
        
        return runtime.getNil();
    }
}
