/*
 * ExpandArrayNode.java - description
 * Created on 27.02.2002, 20:34:08
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
import org.jruby.ast.visitor.NodeVisitor;

/** Represents an expanded argument (*expr).
 * this can be used in a method call, in an array reference or in 
 * a right hand side of an assignment (of course all those are variations
 * on the method call theme with operator flavors).
 * MRI: ArgsCatNode, ArgsPushNode
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class ExpandArrayNode extends AbstractNode {
    static final long serialVersionUID = -4911951839602674675L;

    private final INode expandNode;

    public ExpandArrayNode(INode expandNode) {
        this(expandNode.getPosition(), expandNode);
    }

    /**
     * Constructor for ExpandArrayNode.
     * @param position
     */
    public ExpandArrayNode(ISourcePosition position, INode expandNode) {
        super(position);
        this.expandNode = expandNode;
    }

    /**
     * @see AbstractNode#accept(INodeVisitor)
     */
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitExpandArrayNode(this);
    }

    /**
     * Gets the expandNode.
     * @return Returns a INode
     */
    public INode getExpandNode() {
        return expandNode;
    }
}
