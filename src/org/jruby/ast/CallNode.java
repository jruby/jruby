/*
 * CallNode.java - No description
 * Created on 19.01.2002, 19:05:17
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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
 * A method or operator call.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public final class CallNode extends Node {
    static final long serialVersionUID = -1993752395320088525L;

    private final Node receiverNode;
    private final String name;
    private final Node argsNode;

    public CallNode(SourcePosition position, Node receiverNode, String name, Node argsNode) {
        super(position);
        this.receiverNode = receiverNode;
        this.name = name.intern();
        this.argsNode = argsNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitCallNode(this);
    }

    /**
     * Gets the argsNode.
	 * argsNode representing the method's arguments' value for this call.
     * @return argsNode
     */
    public Node getArgsNode() {
        return argsNode;
    }

    /**
     * Gets the name.
	 * name is the name of the method called
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the receiverNode.
	 * receiverNode is the object on which the method is being called
     * @return receiverNode
     */
    public Node getReceiverNode() {
        return receiverNode;
    }
}
