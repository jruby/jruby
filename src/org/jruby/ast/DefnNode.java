/*
 * DefnNode.java - description
 * Created on 01.03.2002, 14:02:24
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

import org.ablaf.ast.INode;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.runtime.Visibility;

/**
 * method definition node.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class DefnNode extends AbstractNode {
    private String name;
    private INode argsNode;
    private ScopeNode bodyNode;
    private Visibility visibility;
    
    public DefnNode(ISourcePosition position, String name, INode argsNode, ScopeNode bodyNode, Visibility visibility) {
        super(position);
        
        this.name = name.intern();
        this.argsNode = argsNode;
        this.bodyNode = bodyNode;
        this.visibility = visibility;
    }

    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitDefnNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a INode
     */
    public INode getArgsNode() {
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
