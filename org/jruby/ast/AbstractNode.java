/*
 * AbstractNode.java - description
 * Created on 26.02.2002, 16:04:07
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
import org.ablaf.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.*;
import org.jruby.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractNode implements INode {
    private ISourcePosition position;

	/**
	 * constructor without a position.
	 * This should only be used in node constructor where no good position can be computed
	 **/
 	public AbstractNode() {
    }

    public AbstractNode(ISourcePosition position) {
        this.position = position;
        //		if (position == null ) throw new IllegalArgumentException("position is not valid");
    }

    /**
     * @see INode#getPosition()
     */
    public ISourcePosition getPosition() {
        return position;
    }

 
	
}
