/*
 * BignumNode.java - description
 * Created on 23.02.2002, 22:23:09
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

import java.math.BigInteger;

/** Represents a big integer literal.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BignumNode extends Node implements ILiteralNode {
    static final long serialVersionUID = -8646636291868912747L;

    private final BigInteger value;

    public BignumNode(SourcePosition position, BigInteger value) {
        super(position);
        this.value = value;
    }

    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitBignumNode(this);
    }

    /**
     * Gets the value.
     * @return Returns a BigInteger
     */
    public BigInteger getValue() {
        return value;
    }
}
