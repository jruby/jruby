/*
 * ArgsCatNode.java - description
 * 
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

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

public class ArgsCatNode extends Node {
	private static final long serialVersionUID = 3906082365066327860L;

	private final Node firstNode;
    private final Node secondNode;

    public ArgsCatNode(SourcePosition position, Node firstNode, Node secondNode) {
        super(position);
        this.firstNode = firstNode;
        this.secondNode = secondNode;
    }

    public void accept(NodeVisitor visitor) {
        visitor.visitArgsCatNode(this);
    }
    
    public Node getFirstNode() {
        return firstNode;
    }
    
    public Node getSecondNode() {
        return secondNode;
    }
}
