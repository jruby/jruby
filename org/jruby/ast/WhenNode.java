/*
 * WhenNode.java - description
 * Created on 01.03.2002, 23:50:53
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

import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class WhenNode extends AbstractNode {
    private IListNode expressionNodes;
    private INode bodyNode;

    public WhenNode(ISourcePosition position, IListNode expressionNodes, INode bodyNode) {
        super(position);

        this.expressionNodes = expressionNodes;
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitWhenNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode.
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(INode bodyNode) {
        this.bodyNode = bodyNode;
    }

    /**
     * Gets the expressionNodes.
     * @return Returns a IListNode
     */
    public IListNode getExpressionNodes() {
        return expressionNodes;
    }

    /**
     * Sets the expressionNodes.
     * @param expressionNodes The expressionNodes to set
     */
    public void setExpressionNodes(IListNode expressionNodes) {
        this.expressionNodes = expressionNodes;
    }
}