/*
 * WhileNode.java - No description
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
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
import org.ablaf.ast.visitor.INodeVisitor;

/** Represents a while stetement. This could be the both versions:
 * 
 * while &lt;condition&gt;
 *    &lt;body&gt;
 * end
 * 
 * and
 * 
 * &lt;body&gt; 'while' &lt;condition&gt;
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class WhileNode extends AbstractNode {
    private INode conditionNode;
    private INode bodyNode;

    public WhileNode(ISourcePosition position, INode conditionNode, INode bodyNode) {
        super(position);

        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitWhileNode(this);
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
     * Gets the conditionNode.
     * @return Returns a INode
     */
    public INode getConditionNode() {
        return conditionNode;
    }

    /**
     * Sets the conditionNode.
     * @param conditionNode The conditionNode to set
     */
    public void setConditionNode(INode conditionNode) {
        this.conditionNode = conditionNode;
    }
}
