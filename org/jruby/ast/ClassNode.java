/*
 * ClassNode.java - description
 * Created on 28.02.2002, 16:27:26
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

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.ast.visitor.*;
import org.jruby.runtime.*;
import org.ablaf.ast.visitor.INodeVisitor;

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
public class ClassNode extends AbstractNode {
    private String className;
    private ScopeNode bodyNode;
    private INode superNode;
    
    public ClassNode(ISourcePosition position, String className, ScopeNode bodyNode, INode superNode) {
        super(position);
        
        this.className = className;
        this.bodyNode = bodyNode;
        this.superNode = superNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitClassNode(this);
    }
    /**
     * Gets the bodyNode.
     * @return Returns a ScopeNode
     */
    public ScopeNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode.
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(ScopeNode bodyNode) {
        this.bodyNode = bodyNode;
    }

    /**
     * Gets the className.
     * @return Returns a String
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the className.
     * @param className The className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Gets the superNode.
     * @return Returns a INode
     */
    public INode getSuperNode() {
        return superNode;
    }

    /**
     * Sets the superNode.
     * @param superNode The superNode to set
     */
    public void setSuperNode(INode superNode) {
        this.superNode = superNode;
    }
}
