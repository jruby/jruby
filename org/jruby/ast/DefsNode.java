/*
 * DefsNode.java - description
 * Created on 01.03.2002, 15:31:01
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.ast;

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.jruby.ast.visitor.*;

/** Represents a singleton method definition.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefsNode extends AbstractNode {
    private INode receiverNode;
    private String name;
    private INode argsNode;
    private ScopeNode bodyNode;

    public DefsNode(ISourcePosition position, INode receiverNode, String name, INode argsNode, ScopeNode bodyNode) {
        super(position);

        this.receiverNode = receiverNode;
        this.name = name;
        this.argsNode = argsNode;
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDefsNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a INode
     */
    public INode getArgsNode() {
        return argsNode;
    }

    /**
     * Sets the argsNode.
     * @param argsNode The argsNode to set
     */
    public void setArgsNode(INode argsNode) {
        this.argsNode = argsNode;
    }

    /**
     * Gets the bodyNode.
     * @return Returns a ScopeNode
     */
    public ScopeNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode.
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(ScopeNode bodyNode) {
        this.bodyNode = bodyNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the receiverNode.
     * @return Returns a INode
     */
    public INode getReceiverNode() {
        return receiverNode;
    }

    /**
     * Sets the receiverNode.
     * @param receiverNode The receiverNode to set
     */
    public void setReceiverNode(INode receiverNode) {
        this.receiverNode = receiverNode;
    }

}