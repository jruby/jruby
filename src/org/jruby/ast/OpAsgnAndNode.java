/*
 * OpAsgnAndNode.java - description
 * Created on 01.03.2002, 22:51:44
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
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class OpAsgnAndNode extends Node {
    static final long serialVersionUID = 7366271929271260664L;

    private final Node firstNode;
    private final Node secondNode;

    public OpAsgnAndNode(SourcePosition position, Node headNode, Node valueNode) {
        super(position);
        firstNode = headNode;
        secondNode = valueNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitOpAsgnAndNode(this);
    }

    /**
     * Gets the firstNode.
     * @return Returns a Node
     */
    public Node getFirstNode() {
        return firstNode;
    }

    /**
     * Gets the secondNode.
     * @return Returns a Node
     */
    public Node getSecondNode() {
        return secondNode;
    }
}
