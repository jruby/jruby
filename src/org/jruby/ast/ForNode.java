/*
 * ForNode.java - description
 * Created on 01.03.2002, 17:05:53
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

import org.ablaf.ast.INode;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;

/**
 * a For statement.
 * this is almost equivalent to an iternode (the difference being the visibility of the
 * local variables defined in the iterator).
 * 
 * @see IterNode
 * @author  jpetersen
 * @version $Revision$
 */
public class ForNode extends AbstractNode {
    static final long serialVersionUID = -8319863477790150586L;

    private final INode varNode;
    private final INode bodyNode;
    private final INode iterNode;

    public ForNode(ISourcePosition position, INode varNode, INode bodyNode, INode iterNode) {
        super(position);
        this.varNode = varNode;
        this.bodyNode = bodyNode;
        this.iterNode = iterNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitForNode(this);
    }

    /**
     * Gets the bodyNode.
	 * bodyNode is the expression after the in, it is the expression which will have its each() method called.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the iterNode.
	 * iterNode is the block which will be executed when the each() method of the bodyNode will yield.
	 * 
     * @return Returns a INode
     */
    public INode getIterNode() {
        return iterNode;
    }

    /**
     * Gets the varNode.
	 * varNode is the equivalent of the block variable in a regular method call with block type of iteration
     * @return Returns a INode
     */
    public INode getVarNode() {
        return varNode;
    }
}
