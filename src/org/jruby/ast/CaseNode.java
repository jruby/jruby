/*
 * CaseNode.java - description
 * Created on 28.02.2002, 00:33:00
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

/**
 * A Case statement.
 * 
 * Represents a complete case statement, including the body with its
 * when statements.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class CaseNode extends Node {
    static final long serialVersionUID = -2824917272720800901L;

	/**
	 * the case expression.
	 **/
    private final Node caseNode;
	/**
	 * the body of the case.
	 */
    private final Node caseBody;
    
    public CaseNode(SourcePosition position, Node caseNode, Node caseBody) {
        super(position);
        this.caseNode = caseNode;
        this.caseBody = caseBody;
    }

 	/**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitCaseNode(this);
    }

    /**
     * Gets the caseNode.
	 * caseNode is the case expression 
     * @return caseNode
     */
    public Node getCaseNode() {
        return caseNode;
    }

    /**
     * Gets the first whenNode.
	 * the body of the case statement, the first of a list of WhenNodes
     * @return whenNode
     */
    public Node getFirstWhenNode() {
        return caseBody;
    }
}
