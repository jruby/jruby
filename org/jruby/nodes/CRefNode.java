/*
 * CRefNode.java - No description
 * Created on 05. November 2001, 21:45
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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
package org.jruby.nodes;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/** CRefNode represents an element in the nested module/class hierarchy.
 * 
 * Example:
 * 
 * <pre>
 * module JRuby
 *    class ExampleClass
 *       #1
 *    end
 * end
 * </pre>
 * 
 * At point #1 there is the CRef structure:
 * 
 * <pre>
 * CRefNode -> class = ExampleClass
 *          -> next = CRefNode -> class = JRuby
 *                             -> next = CRefNode -> class = TopSelf
 *                                                -> next = null
 * </pre>
 * 
 * TODO: This class should be moved to a simple LinkedList in org.jruby.runtime
 * because there is no relation to the AST.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class CRefNode extends Node {
    public CRefNode(Node nextNode) {
        this(null, nextNode);
    }

    public CRefNode(RubyObject classValue, Node nextNode) {
        super(Constants.NODE_CREF, classValue, null, nextNode);
    }

    public CRefNode cloneCRefNode() {
        return new CRefNode(getClassValue(), getNextNode() != null ? ((CRefNode) getNextNode()).cloneCRefNode() : null);
    }

    /** push a new RubyObject to this CRefNode
     *
     */
    public void push(RubyObject newClassValue) {
        setNextNode(new CRefNode(getClassValue(), getNextNode()));

        setClassValue(newClassValue);
    }

    /** pop a RubyObject from this CRefNode
     *
     */
    public void pop() {
        setClassValue(getNextNode().getClassValue());
        setNextNode(getNextNode().getNextNode());
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitCRefNode(this);
    }
}