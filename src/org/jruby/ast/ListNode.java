/*
 * ListNode.java - Base of all nodes which are a list
 * 
 * Copyright (C) 2004 Thomas E Enebo
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jruby.lexer.yacc.SourcePosition;

/**
 * @author enebo
 */
public abstract class ListNode extends Node {
    private List list = null;

	public ListNode() { 
		super(); 
	}

	public ListNode(SourcePosition position) {
		super(position);
	}
	
    public ListNode add(Node node) {
        if (list == null) {
            list = new ArrayList();
        }
        list.add(node);
        return this;
    }

    public Iterator iterator() {
        return list == null ? Collections.EMPTY_LIST.iterator() : 
        	list.iterator();
    }
    
    public int size() {
        return list == null ? 0 : list.size();
    }
    
    public ListNode addAll(ListNode other) {
        if (other != null) {
        	for (Iterator iter = other.iterator(); iter.hasNext();) {
                add((Node) iter.next());
            }
        }
        return this;
    }
    
    public Node getLast() {
    	return list == null ? null : (Node) list.get(list.size() - 1);
    }
    
    public String toString() {
    	if (list == null) {
    		return "";
    	}
    	StringBuffer b = new StringBuffer();
    	for (int i = 0; i < list.size(); i++) {
    		b.append(list.get(i));
    	}
    	return b.toString();
    }
}
