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

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.jruby.ast.visitor.*;

/**
 * a Range in a boolean expression.
 * named after a FlipFlop component in electronic I believe.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class FlipNode extends AbstractNode {
    private INode beginNode;
    private INode endNode;
    private boolean exclusive;
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
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitFlipNode(this);
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
     * Sets the beginNode.
	 * beginNode will set the FlipFlop when it is true while the FlipFlop is unset
     * @param beginNode The beginNode to set
     */
    public void setBeginNode(INode beginNode) {
        this.beginNode = beginNode;
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
     * Sets the endNode.
	 * endNode will reset the FlipFlop when it is true while the FlipFlop is set.
     * @param endNode The endNode to set
     */
    public void setEndNode(INode endNode) {
        this.endNode = endNode;
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
     * Sets the exclusive.
     * @param exclusive The exclusive to set
     */
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
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