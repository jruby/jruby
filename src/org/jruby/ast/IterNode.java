/*
 * IterNode.java - description
 * Created on 01.03.2002, 23:23:50
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
 * @see ForNode
 * @author  jpetersen
 * @version $Revision$
 */
public class IterNode extends Node {
    static final long serialVersionUID = -9181965000180892184L;

    private final Node varNode;
    private final Node bodyNode;
    private Node iterNode;

    public IterNode(SourcePosition position, Node varNode, Node bodyNode, Node iterNode) {
        super(position);
        this.varNode = varNode;
        this.bodyNode = bodyNode;
        this.iterNode = iterNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitIterNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the iterNode.
     * @return Returns a Node
     */
    public Node getIterNode() {
        return iterNode;
    }

    /**
     * Sets the iterNode.
     * @param iterNode The iterNode to set
     */
    public void setIterNode(Node iterNode) {
        this.iterNode = iterNode;
    }

    /**
     * Gets the varNode.
     * @return Returns a Node
     */
    public Node getVarNode() {
        return varNode;
    }
}
