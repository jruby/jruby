/*
 * FixnumNode.java - description
 * Created on 23.02.2002, 19:04:53
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

import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;

/** Represents an integer literal.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class FixnumNode extends AbstractNode implements ILiteralNode {
    private final long value;

    public FixnumNode(ISourcePosition position, long value) {
        super(position);
        this.value = value;
    }

    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitFixnumNode(this);
    }

    /**
     * Gets the value.
     * @return Returns a long
     */
    public long getValue() {
        return value;
    }
}
