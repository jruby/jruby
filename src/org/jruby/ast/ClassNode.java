/*
 * ClassNode.java - description
 * Created on 28.02.2002, 16:27:26
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
 * A class statement.
 * A class statement is defined by its name, its supertype and its body.
 * The body is a separate naming scope.
 * This node is for a regular class definition, Singleton classes get their own
 * node, the SClassNode
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class ClassNode extends Node {
    static final long serialVersionUID = -1369424045737867587L;

    private final String className;
    private final ScopeNode bodyNode;
    private final Node superNode;
    
    public ClassNode(SourcePosition position, String className, ScopeNode bodyNode, Node superNode) {
        super(position);
        this.className = className;
        this.bodyNode = bodyNode;
        this.superNode = superNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitClassNode(this);
    }
    /**
     * Gets the bodyNode.
     * @return Returns a ScopeNode
     */
    public ScopeNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the className.
     * @return Returns a String
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the superNode.
     * @return Returns a Node
     */
    public Node getSuperNode() {
        return superNode;
    }
}
