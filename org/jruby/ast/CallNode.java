/*
 * CallNode.java - No description
 * Created on 19.01.2002, 19:05:17
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;

/**
 * A method or operator call.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class CallNode extends AbstractNode {
    private INode receiverNode;
    private String name;
    private IListNode argsNode;

    public CallNode(ISourcePosition position, INode receiverNode, String name, IListNode argsNode) {
        super(position);

        this.receiverNode = receiverNode;
        this.name = name;
        this.argsNode = argsNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitCallNode(this);
    }

    /**
     * Gets the argsNode.
	 * argsNode representing the method's arguments' value for this call.
     * @return argsNode
     */
    public IListNode getArgsNode() {
        return argsNode;
    }

    /**
     * Sets the argsNode.
	 * argsNode representing the method's arguments' value for this call.
     * @param argsNode The argsNode to set
     */
    public void setArgsNode(IListNode argsNode) {
        this.argsNode = argsNode;
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
     * Sets the name.
	 * name is the name of the method called
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the receiverNode.
	 * receiverNode is the object on which the method is being called
     * @return receiverNode
     */
    public INode getReceiverNode() {
        return receiverNode;
    }

    /**
     * Sets the receiverNode.
	 * receiverNode is the object on which the method is being called
     * @param receiverNode The receiverNode to set
     */
    public void setReceiverNode(INode receiverNode) {
        this.receiverNode = receiverNode;
    }
}
