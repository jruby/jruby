/*
 * DRegexpNode.java - description
 * Created on 24.02.2002, 17:27:42
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
import org.ablaf.ast.visitor.INodeVisitor;

/**
 *	Dynamic regexp node.
 *	a regexp is dynamic if it contains some expressions which will need to be evaluated
 *	everytime the regexp is used for a match
 * @author  jpetersen
 * @version $Revision$
 */
public class DRegexpNode extends AbstractNode implements IListNode, ILiteralNode {
    private List list;

    private int options;
    private boolean once;
    
    public DRegexpNode(ISourcePosition position) {
        this(position, 0, false);
    }

    public DRegexpNode(ISourcePosition position, int options, boolean once) {
        super(position);

        this.options = options;
        this.once = once;
    }

    /**
     * @see IListNode#add(INode)
     */
    public IListNode add(INode node) {
        if (list == null) {
            list = new ArrayList();
        }
        list.add(node);

        return this;
    }

    /**
     * @see IListNode#iterator()
     */
    public Iterator iterator() {
        if (list == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return list.iterator();
        }
    }
    
    /**
     * @see org.jruby.ast.types.IListNode#size()
     */
    public int size() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitDRegxNode(this);
    }

    /**
     * Gets the once.
     * @return Returns a boolean
     */
    public boolean getOnce() {
        return once;
    }

    /**
     * Sets the once.
     * @param once The once to set
     */
    public void setOnce(boolean once) {
        this.once = once;
    }

    /**
     * Gets the options.
     * @return Returns a int
     */
    public int getOptions() {
        return options;
    }

    /**
     * Sets the options.
     * @param options The options to set
     */
    public void setOptions(int options) {
        this.options = options;
    }
}
