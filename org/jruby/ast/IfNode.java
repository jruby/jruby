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

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.jruby.ast.visitor.*;

/**
 * an 'if' statement.
 * @author  jpetersen
 * @version $Revision$
 */
public class IfNode extends AbstractNode {
    private INode condition;
    private INode thenBody;
    private INode elseBody;

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
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitIfNode(this);
    }

    /**
     * Gets the condition.
     * @return Returns a INode
     */
    public INode getCondition() {
        return condition;
    }

    /**
     * Sets the condition.
     * @param condition The condition to set
     */
    public void setCondition(INode condition) {
        this.condition = condition;
    }

    /**
     * Gets the elseBody.
     * @return Returns a INode
     */
    public INode getElseBody() {
        return elseBody;
    }

    /**
     * Sets the elseBody.
     * @param elseBody The elseBody to set
     */
    public void setElseBody(INode elseBody) {
        this.elseBody = elseBody;
    }

    /**
     * Gets the thenBody.
     * @return Returns a INode
     */
    public INode getThenBody() {
        return thenBody;
    }

    /**
     * Sets the thenBody.
     * @param thenBody The thenBody to set
     */
    public void setThenBody(INode thenBody) {
        this.thenBody = thenBody;
    }
}
