/*
 * CDeclNode.java - description
 * Created on 28.02.2002, 16:04:27
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
import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;

/**
 * Declaration (and assignment) of a Constant.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class ConstDeclNode extends AbstractNode implements IAssignableNode {
    private final String name;
    private INode valueNode;

    public ConstDeclNode(ISourcePosition position, String name, INode valueNode) {
        super(position);
        this.name = name;
        this.valueNode = valueNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitConstDeclNode(this);
    }

    /**
     * Gets the name.
	 * name is the constant Name, it normally starts with a Capital
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the valueNode.
	 * The node to evaluate to get the actual value to assign to the constant
     * @return valueNode
     */
    public INode getValueNode() {
        return valueNode;
    }

    /**
     * Sets the valueNode.
	 * The node to evaluate to get the actual value to assign to the constant
     * @param valueNode The valueNode to set
     */
    public void setValueNode(INode valueNode) {
        this.valueNode = valueNode;
    }
}
