/*
 * DRegexpNode.java - description
 * Created on 24.02.2002, 17:27:42
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

/**
 *	Dynamic regexp node.
 *	a regexp is dynamic if it contains some expressions which will need to be evaluated
 *	everytime the regexp is used for a match
 * @author  jpetersen
 * @version $Revision$
 */
public class DRegexpNode extends ListNode implements ILiteralNode {
    static final long serialVersionUID = 7307853378003210140L;

    private final int options;
    private final boolean once;
    
    public DRegexpNode(SourcePosition position) {
        this(position, 0, false);
    }

    public DRegexpNode(SourcePosition position, int options, boolean once) {
        super(position);

        this.options = options;
        this.once = once;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDRegxNode(this);
    }

    /**
     * Gets the once.
     * @return Returns a boolean
     */
    public boolean getOnce() {
        return once;
    }

    /**
     * Gets the options.
     * @return Returns a int
     */
    public int getOptions() {
        return options;
    }
}
