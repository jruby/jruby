/*
 * GlobalAsgnNode.java - description
 * Created on 01.03.2002, 16:46:20
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

import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

/**
 * Represents an assignment to a global variable.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class GlobalAsgnNode extends AssignableNode implements INameNode {
    static final long serialVersionUID = 2278414591762936906L;

    private final String name;

    public GlobalAsgnNode(SourcePosition position, String name, Node valueNode) {
        super(position);

        this.name = name;
        setValueNode(valueNode);
    }

    /**
     * RubyMethod used by visitors.
     * accepts the visitor 
     * @param iVisitor the visitor to accept
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitGlobalAsgnNode(this);
    }
    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }
}
