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

import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;

/** An AndNode represents a && operator.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class AndNode extends AbstractNode {
    static final long serialVersionUID = 1716928209521564017L;

    private final INode firstNode;
    private final INode secondNode;

    public AndNode(ISourcePosition position, INode firstNode, INode secondNode) {
        super(position);
        this.firstNode = firstNode;
        this.secondNode = secondNode;
    }

    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitAndNode(this);
    }

    /**
     * Gets the secondNode.
     * @return Returns a Node
     */
    public INode getSecondNode() {
        return secondNode;
    }

    /**
     * Gets the firstNode.
     * @return Returns a Node
     */
    public INode getFirstNode() {
        return firstNode;
    }
}
