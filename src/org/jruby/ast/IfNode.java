/*
 * IfNode.java - description
 * Created on 28.02.2002, 22:45:58
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
 * an 'if' statement.
 * @author  jpetersen
 * @version $Revision$
 */
public class IfNode extends AbstractNode {
    private final INode condition;
    private final INode thenBody;
    private final INode elseBody;

    public IfNode(ISourcePosition position, INode condition, INode thenBody, INode elseBody) {
        super(position);
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitIfNode(this);
    }

    /**
     * Gets the condition.
     * @return Returns a INode
     */
    public INode getCondition() {
        return condition;
    }

    /**
     * Gets the elseBody.
     * @return Returns a INode
     */
    public INode getElseBody() {
        return elseBody;
    }

    /**
     * Gets the thenBody.
     * @return Returns a INode
     */
    public INode getThenBody() {
        return thenBody;
    }
}
