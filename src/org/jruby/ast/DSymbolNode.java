/*
 * SymbolNode.java - description
 * Created on 23.02.2002, 19:28:20
 *
 * Copyright (C) 2004 Thomas E. Enebo
 * Thomas E. Enebo <enebo@acm.org>
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
 * Node representing symbol in a form like ':"3jane"'.
 * 
 * @author enebo
 */
public class DSymbolNode extends Node {
	DStrNode node;

	public DSymbolNode(SourcePosition position, DStrNode node) {
		super(position);
		this.node = node;
	}

	public void accept(NodeVisitor visitor) {
		visitor.visitDSymbolNode(this);
	}
	
	public DStrNode getNode() {
		return node;
	}
}
