/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * All Nodes which have a list representation inherit this.  This is also used
 * as generic container for additional information that is not directly evaluated.
 * In particular, f_arg production rule uses this to capture arg information for
 * the editor projects who want position info saved.
 */
public class ListNode extends Node {
    private static final long serialVersionUID = 1L;
    
    private List list = null;

	public ListNode(ISourcePosition position) {
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
        return list == null ? EMPTY_LIST.iterator() : list.iterator();
    }
    
    public ListIterator reverseIterator() {
    	return list == null ? EMPTY_LIST.listIterator() : list.listIterator(list.size());
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
        String string = super.toString();
    	if (list == null) {
    		return string + ": {}";
    	}
    	StringBuffer b = new StringBuffer();
    	for (int i = 0; i < list.size(); i++) {
    		b.append(list.get(i));
            if (i + 1 < list.size()) {
                b.append(", ");
            }
    	}
    	return string + ": {" + b.toString() + "}";
    }
    
    public List childNodes() {
    	return list;
    }
    
    public Instruction accept(NodeVisitor visitor) {
        throw new RuntimeException("Base class ListNode should never be evaluated");
    }
    
    public Node get(int idx) {
        return (Node)list.get(idx);
    }
}
