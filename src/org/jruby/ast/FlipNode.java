/*
 * FlipNode.java - No description
 * Created on 05. November 2001, 21:45
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
import org.jruby.ast.visitor.NodeVisitor;

/**
 * a Range in a boolean expression.
 * named after a FlipFlop component in electronic I believe.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class FlipNode extends AbstractNode {
    static final long serialVersionUID = -4735579451657299802L;

    private final INode beginNode;
    private final INode endNode;
    private final boolean exclusive;
    private int count;
    
    public FlipNode(ISourcePosition position, INode beginNode, INode endNode, boolean exclusive) {
        super(position);
        this.beginNode = beginNode;
        this.endNode = endNode;
        this.exclusive = exclusive;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitFlipNode(this);
    }

    /**
     * Gets the beginNode.
	 * beginNode will set the FlipFlop the first time it is true
     * @return Returns a INode
     */
    public INode getBeginNode() {
        return beginNode;
    }

    /**
     * Gets the endNode.
	 * endNode will reset the FlipFlop when it is true while the FlipFlop is set.
     * @return Returns a INode
     */
    public INode getEndNode() {
        return endNode;
    }

    /**
     * Gets the exclusive.
	 * if the range is a 2 dot range it is false if it is a three dot it is true
     * @return Returns a boolean
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Gets the count.
     * @return Returns a int
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count.
     * @param count The count to set
     */
    public void setCount(int count) {
        this.count = count;
    }
}
