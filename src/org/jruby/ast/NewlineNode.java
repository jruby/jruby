/*
 * NewlineNode.java - description
 * Created on 28.02.2002, 00:19:51
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
 * A new (logical) source code line.
 * This is used to change the value of the ruby interpreter        
 * source and line values.
 * There is one such node for each logical line.  Logical line differs
 * from physical line in that a ';' can be used to make several logical
 * line out of a physical line and a physical line if it is in a comment
 * or in a string does not necessarily correspond to a physical line.
 * This is normally a wrapper around another more significant node.
 * The parser generates such a node around each separate statement.  
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class NewlineNode extends Node {
    static final long serialVersionUID = -6180129177863553832L;

    private final Node nextNode;

    public NewlineNode(SourcePosition position, Node nextNode) {
        super(position);

        this.nextNode = nextNode;
    }

    /**
     * RubyMethod used by visitors.
     * accepts the visitor
     * @param iVisitor the visitor to accept
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitNewlineNode(this);
    }

    /**
     * Gets the nextNode.
     * @return Returns a Node
     */
    public Node getNextNode() {
        return nextNode;
    }

}
