/*
 * DotNode.java - description
 * Created on 01.03.2002, 15:42:44
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

/** Represents a range literal.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DotNode extends Node {
    static final long serialVersionUID = 2763797850980107429L;

    private final Node beginNode;
    private final Node endNode;
    private final boolean exclusive;

    public DotNode(SourcePosition position, Node beginNode, Node endNode, boolean exclusive) {
        super(position);
        this.beginNode = beginNode;
        this.endNode = endNode;
        this.exclusive = exclusive;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDotNode(this);
    }

    /**
     * Gets the beginNode.
     * @return Returns a Node
     */
    public Node getBeginNode() {
        return beginNode;
    }

    /**
     * Gets the endNode.
     * @return Returns a Node
     */
    public Node getEndNode() {
        return endNode;
    }

    /**
     * Gets the exclusive.
     * @return Returns a boolean
     */
    public boolean isExclusive() {
        return exclusive;
    }
}
