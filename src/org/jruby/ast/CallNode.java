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

import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.types.IListNode;
import org.jruby.ast.visitor.NodeVisitor;

/**
 * A method or operator call.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public final class CallNode extends AbstractNode {
    static final long serialVersionUID = -1993752395320088525L;

    private final INode receiverNode;
    private final String name;
    private final IListNode argsNode;

    public CallNode(ISourcePosition position, INode receiverNode, String name, IListNode argsNode) {
        super(position);
        this.receiverNode = receiverNode;
        this.name = name.intern();
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
    public INode getReceiverNode() {
        return receiverNode;
    }
}
