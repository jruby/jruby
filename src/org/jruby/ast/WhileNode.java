/*
 * WhileNode.java - No description
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

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
public class WhileNode extends Node {
    static final long serialVersionUID = -5355364190446060873L;

    private final Node conditionNode;
    private final Node bodyNode;
    private final boolean evaluateAtStart;

    public WhileNode(SourcePosition position, Node conditionNode, Node bodyNode) {
	this(position, conditionNode, bodyNode, true);
    }

    public WhileNode(SourcePosition position, Node conditionNode, Node bodyNode,
            boolean evalAtStart) {
        super(position);
        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
        this.evaluateAtStart = evalAtStart;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitWhileNode(this);
    }
    /**
     * Gets the bodyNode.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the conditionNode.
     * @return Returns a Node
     */
    public Node getConditionNode() {
        return conditionNode;
    }
    
    /**
     * Determine whether this is while or do while
     * @return true if you are a while, false if do while
     */
    public boolean evaluateAtStart() {
        return evaluateAtStart;
    }
}
