/*
 * StrTermNode.java - description
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

public class StrTermNode extends Node {
    int func;
    int termParen;
    int nest;

    /**
     * @param position
     */
    public StrTermNode(SourcePosition position, int func, int term, int paren) {
        super(position);

        this.func = func;
        this.termParen = term | (paren << 16);  
        this.nest = 0;
    }

    public void accept(NodeVisitor visitor) {
    }

    public int getFunc() {
        return func;
    }
    
    public void setFunc(int func) {
        this.func = func;
    }
    
    public int getTerm() {
        // We only want first two bytes of term paren (the term part)
        return (short)termParen;
    }
    
    public int getParen() {
        return termParen >> 16;
    }
    
    public int getNest() {
        return nest;
    }
    
    public void setNest(int nest) {
        this.nest = nest;
    }
}
