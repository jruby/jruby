/*
 * BackRefNode.java - description
 * Created on 25.02.2002, 00:11:17
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.ablaf.common.*;
import org.jruby.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;

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
public class BackRefNode extends AbstractNode {
	/**
	 * the character which generated the backreference
	 **/
    private char type;

    public BackRefNode(ISourcePosition position, char type) {
        super(position);

        this.type = type;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitBackRefNode(this);
    }

    /**
     * Gets the type.
	 * the type is the character which generates the backreference
     * @return type
     */
    public char getType() {
        return type;
    }

    /**
     * Sets the type.
	 * the type is the character which generates the backreference
     * @param type The type to set
     */
    public void setType(char type) {
        this.type = type;
    }
}
