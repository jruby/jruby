/*
 * Node.java - Base of all Nodes
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

import java.io.Serializable;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class Node implements Serializable {
    static final long serialVersionUID = -5962822607672530224L;

    private SourcePosition position;

	/**
	 * constructor without a position.
	 * This should only be used in node constructor where no good position can be computed
	 **/
 	public Node() {
 		this(null);
    }

    public Node(SourcePosition position) {
        this.position = position;
    }

    /**
     * Location of this node within the source
     */
    public SourcePosition getPosition() {
        return position;
    }
    
	public abstract void accept(NodeVisitor visitor);
}
