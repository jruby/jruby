/*
 * InstVarAsgnNode.java - description
 * Created on 01.03.2002, 17:19:12
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

import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.types.IAssignableNode;
import org.jruby.ast.visitor.NodeVisitor;

/** Represents an instance variable assignment.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class InstAsgnNode extends AbstractNode implements IAssignableNode {
    private final String name;
    private INode valueNode;

    /**
     * Construtor.
     * @param iName the name of the instance variable
     * @param iValueNode the value of the variable
     **/
    public InstAsgnNode(ISourcePosition position, String iName, INode iValueNode) {
        super(position);
        name = iName;
        valueNode = iValueNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitInstAsgnNode(this);
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the valueNode.
     * @return Returns a INode
     */
    public INode getValueNode() {
        return valueNode;
    }

    /**
     * Sets the valueNode.
     * @param valueNode The valueNode to set
     */
    public void setValueNode(INode valueNode) {
        this.valueNode = valueNode;
    }
}
