/*
 * DStrNode.java - description
 * Created on 24.02.2002, 16:29:24
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

import java.util.*;

import org.ablaf.ast.*;
import org.ablaf.common.*;

import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;

/**
 * a Dynamic String node.
 * A string which contains some dynamic elements which needs to be evaluated (introduced by #).
 * @author  jpetersen
 * @version $Revision$
 */
public class DStrNode extends AbstractNode implements IListNode, ILiteralNode {
    private List nodeList;

    public DStrNode(ISourcePosition position) {
        super(position);
    }

    public IListNode add(INode node) {
        if (nodeList == null) {
            nodeList = new LinkedList();
        }
        nodeList.add(node);
        
        return this;
    }

    public Iterator iterator() {
        if (nodeList == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return nodeList.iterator();
        }
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDStrNode(this);
    }
}
