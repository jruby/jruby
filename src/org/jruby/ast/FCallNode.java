/*
 * FCallNode.java - description
 * Created on 01.03.2002, 16:28:29
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
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.ast.types.IListNode;

/** Represents a method call with self as receiver.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class FCallNode extends AbstractNode {
    static final long serialVersionUID = 3590332973770104094L;

    private final String name;
    private final IListNode argsNode;

    public FCallNode(ISourcePosition position, String name, IListNode argsNode) {
        super(position);
        this.name = name.intern();
        this.argsNode = argsNode;

        if (! (argsNode instanceof ArrayNode)) {
            if (argsNode != null) {
                System.out.println("" + argsNode.getClass());
            }
        }
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitFCallNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a INode
     */
    public IListNode getArgsNode() {
        return argsNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }
}
