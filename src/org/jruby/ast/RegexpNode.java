/*
 * RegexpNode.java - description
 * Created on 23.02.2002, 19:29:50
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

import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

/** Represents a simple regular expression literal.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RegexpNode extends Node implements ILiteralNode {
    static final long serialVersionUID = -1566813018564622077L;

    private final String value;
    private final int options;
    
    public RegexpNode(SourcePosition position, String value, int options) {
        super(position);
        
        this.value = value;
        this.options = options;
    }

    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitRegexpNode(this);
    }

    /**
     * Gets the options.
     * @return Returns a int
     */
    public int getOptions() {
        return options;
    }

    /**
     * Gets the value.
     * @return Returns a String
     */
    public String getValue() {
        return value;
    }
}
