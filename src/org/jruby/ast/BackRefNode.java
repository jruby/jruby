/*
 * BackRefNode.java - description
 * Created on 25.02.2002, 00:11:17
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
 *	Regexp backref.
 *	generated when one of the following special global variables are found
 *
 *
 *    - $&amp; last succesfull match
 *     
 *    - $+ highest numbered group matched in last successful match.
 *     
 *    - $` what precedes the last successful match
 *    
 *    - $' what follows the last successful match
 *
 *	
 * @author  jpetersen
 * @version $Revision$
 */
public class BackRefNode extends Node {
    static final long serialVersionUID = 5321267679438359590L;

	/**
	 * the character which generated the backreference
	 **/
    private final char type;

    public BackRefNode(SourcePosition position, char type) {
        super(position);
        this.type = type;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitBackRefNode(this);
    }

    /**
     * Gets the type.
	 * the type is the character which generates the backreference
     * @return type
     */
    public char getType() {
        return type;
    }
}
