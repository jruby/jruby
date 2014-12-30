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
 * Copyright (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Node that represents an assignment of either an array element or attribute.
 */
public class AttrAssignNode extends Node implements INameNode, IArgumentNode {
    protected final Node receiverNode;
    private String name;
    private Node argsNode;

    public AttrAssignNode(ISourcePosition position, Node receiverNode, String name, Node argsNode) {
        super(position);
        
        assert receiverNode != null : "receiverNode is not null";
        // TODO: At least ParserSupport.attrset passes argsNode as null.  ImplicitNil is wrong magic for 
        // setupArgs since it will IRubyObject[] { nil }.  So we need to figure out a nice fast
        // null pattern for setupArgs.
        // assert argsNode != null : "receiverNode is not null";
        
        this.receiverNode = receiverNode;
        this.name = name;
        this.argsNode = argsNode;
    }

    public NodeType getNodeType() {
        return NodeType.ATTRASSIGNNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param visitor the visitor
     **/
    public Object accept(NodeVisitor visitor) {
        return visitor.visitAttrAssignNode(this);
    }

    /**
     * Gets the name.
     * name is the name of the method called
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the receiverNode.
     * receiverNode is the object on which the method is being called
     * @return receiverNode
     */
    public Node getReceiverNode() {
        return receiverNode;
    }
    
    /**
     * Gets the argsNode.
     * argsNode representing the method's arguments' value for this call.
     * @return argsNode
     */
    public Node getArgsNode() {
        return argsNode;
    }
    
    /**
     * Set the argsNode
     * 
     * @param argsNode set the arguments for this node.
     */
    public Node setArgsNode(Node argsNode) {
        this.argsNode = argsNode;

        return this;
    }

    public List<Node> childNodes() {
        return Node.createList(receiverNode, argsNode);
    }
}
