/*
 * SClassNode.java - description
 * Created on 01.03.2002, 22:12:05
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

/** Singleton class definition.
 * 
 * <pre>
 * class &lt;&lt; anObject
 * 
 * end
 * </pre>
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class SClassNode extends AbstractNode {
    static final long serialVersionUID = -3706492163082062224L;

    private final INode receiverNode;
    private final ScopeNode bodyNode;

    public SClassNode(ISourcePosition position, INode recvNode, ScopeNode bodyNode) {
        super(position);
        this.receiverNode = recvNode;
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitSClassNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a ScopeNode
     */
    public ScopeNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the receiverNode.
     * @return Returns a INode
     */
    public INode getReceiverNode() {
        return receiverNode;
    }
}
