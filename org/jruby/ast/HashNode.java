/*
 * HashNode.java - description
 * Created on 01.03.2002, 17:12:18
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

import org.ablaf.common.*;

import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;

/**
 * a Literal Hash.
 * this can represent either a {a=&amp;b, c=&amp;d} type expression or the list of default 
 * values in a method call.
 * @author  jpetersen
 * @version $Revision$
 */
public class HashNode extends AbstractNode {
    private IListNode listNode;
    
    public HashNode(IListNode listNode) {
        this(null, listNode);
    }

    public HashNode(ISourcePosition position, IListNode listNode) {
        super(position);

        this.listNode = listNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitHashNode(this);
    }

    /**
     * Gets the listNode.
     * @return Returns a IListNode
     */
    public IListNode getListNode() {
        return listNode;
    }

    /**
     * Sets the listNode.
     * @param listNode The listNode to set
     */
    public void setListNode(IListNode listNode) {
        this.listNode = listNode;
    }
}
