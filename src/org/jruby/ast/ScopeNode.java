/*
 * ScopeNode.java - No description
 * Created on 14.01.2002, 20:48:10
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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

import java.util.List;

/**
 * Scope in the parse tree.
 * indicates where in the parse tree a new scope should be started when evaling.
 * Unlike many node this is not created directly as part of the parce process,
 * rather it is created as a side effect of other things like creating a ClassNode
 * or SClassNode.  It can also be created by evaling a DefnNode or DefsNode as
 * part of the call to copyNodeScope.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ScopeNode extends Node {
    static final long serialVersionUID = 3694868125861223886L;

    private final List localNames;
    private final Node bodyNode;

    public ScopeNode(List table, Node bodyNode) {
		super();
		this.localNames = table;
		this.bodyNode = bodyNode;
    }

    public ScopeNode(SourcePosition position, List table, Node bodyNode) {
        super(position);
        this.localNames =  table;
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitScopeNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the localNames.
     * @return Returns a List
     */
    public List getLocalNames() {
        return localNames;
    }
}
