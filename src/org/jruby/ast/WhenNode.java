/*
 * WhenNode.java - description
 * Created on 01.03.2002, 23:50:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo
 * Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.types.IListNode;
import org.jruby.ast.visitor.NodeVisitor;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class WhenNode extends AbstractNode {
    static final long serialVersionUID = 9099987602002276708L;

    private final IListNode expressionNodes;
    private final INode bodyNode;
    private final INode nextCase;

    public WhenNode(ISourcePosition position, IListNode expressionNodes, INode bodyNode, INode nextCase) {
        super(position);
        this.expressionNodes = expressionNodes;
        this.bodyNode = bodyNode;
        this.nextCase = nextCase;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitWhenNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }
    
    /**
     * Gets the next case node (if any).
     */
    public INode getNextCase() {
        return nextCase;
    }

    /**
     * Gets the expressionNodes.
     * @return Returns a IListNode
     */
    public IListNode getExpressionNodes() {
        return expressionNodes;
    }
}
