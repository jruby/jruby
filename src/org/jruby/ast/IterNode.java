/*
 * IterNode.java - description
 * Created on 01.03.2002, 23:23:50
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

import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;

/**
 *
 * @see ForNode
 * @author  jpetersen
 * @version $Revision$
 */
public class IterNode extends AbstractNode {
    static final long serialVersionUID = -9181965000180892184L;

    private final INode varNode;
    private final INode bodyNode;
    private INode iterNode;

    public IterNode(ISourcePosition position, INode varNode, INode bodyNode, INode iterNode) {
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
        ((NodeVisitor)iVisitor).visitIterNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the iterNode.
     * @return Returns a INode
     */
    public INode getIterNode() {
        return iterNode;
    }

    /**
     * Sets the iterNode.
     * @param iterNode The iterNode to set
     */
    public void setIterNode(INode iterNode) {
        this.iterNode = iterNode;
    }

    /**
     * Gets the varNode.
     * @return Returns a INode
     */
    public INode getVarNode() {
        return varNode;
    }
}
