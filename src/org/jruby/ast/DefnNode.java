/*
 * DefnNode.java - description
 * Created on 01.03.2002, 14:02:24
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
import org.jruby.runtime.Visibility;

/**
 * method definition node.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class DefnNode extends Node {
    static final long serialVersionUID = -7634791007500033454L;

    private String name;
    private Node argsNode;
    private ScopeNode bodyNode;
    private Visibility visibility;
    
    public DefnNode(SourcePosition position, String name, Node argsNode, ScopeNode bodyNode, Visibility visibility) {
        super(position);
        
        this.name = name.intern();
        this.argsNode = argsNode;
        this.bodyNode = bodyNode;
        this.visibility = visibility;
    }

    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDefnNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a Node
     */
    public Node getArgsNode() {
        return argsNode;
    }

    /**
     * Gets the bodyNode.
     * @return Returns a ScopeNode
     */
    public ScopeNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the noex.
     * @return Returns a int
     */
    public Visibility getVisibility() {
        return visibility;
    }
}
