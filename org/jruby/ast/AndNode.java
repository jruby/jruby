/*
 * AndNode.java - description
 * Created on 26.02.2002, 16:28:08
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
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

/** An AndNode represents a && operator.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class AndNode extends AbstractNode {
    private INode firstNode;
    private INode secondNode;

    public AndNode(ISourcePosition position, INode firstNode, INode secondNode) {
        super(position);

        this.firstNode = firstNode;
        this.secondNode = secondNode;
    }

    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitAndNode(this);
    }

    /**
     * Gets the secondNode.
     * @return Returns a Node
     */
    public INode getSecondNode() {
        return secondNode;
    }

    /**
     * Sets the secondNode.
     * @param secondNode The secondNode to set
     */
    public void setSecondNode(INode secondNode) {
        this.secondNode = secondNode;
    }

    /**
     * Gets the firstNode.
     * @return Returns a Node
     */
    public INode getFirstNode() {
        return firstNode;
    }

    /**
     * Sets the firstNode.
     * @param firstNode The firstNode to set
     */
    public void setFirstNode(INode firstNode) {
        this.firstNode = firstNode;
    }
}