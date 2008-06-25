/*
 ***** BEGIN LICENSE BLOCK *****
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
import java.util.List;
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
    static final Node[] EMPTY = new Node[0];
    protected Node[] list;

    /**
     * Create a new ListNode.
     * 
     * @param id type of listnode
     * @param firstNode first element of the list
     */
    public ListNode(ISourcePosition position, NodeType id, Node firstNode) {
        this(position, id);
        
        list = new Node[1];
        list[0] = firstNode;
    }
    
    public ListNode(ISourcePosition position, NodeType id) {
        super(position, id);
        
        list = EMPTY;
    }

    public ListNode(ISourcePosition position) {
        this(position, NodeType.LISTNODE);
    }

    private int grow(int amount) {
        int oldSize = list.length;
        Node newList[] = new Node[oldSize + amount];

        System.arraycopy(list, 0, newList, 0, oldSize);

        list = newList;
        
        return oldSize;
    }
    
    public ListNode add(Node node) {
        // Ruby Grammar productions return plenty of nulls.
        if (node == null) return this;

        grow(1);
        list[list.length - 1] = node;

        if (getPosition() == null) {
            setPosition(node.getPosition());
        } else {
            setPosition(getPosition().union(node.getPosition()));
        }

        return this;
    }
    
    public ListNode prepend(Node node) {
        // Ruby Grammar productions return plenty of nulls.
        if (node == null) return this;

        Node newList[] = new Node[list.length + 1];

        System.arraycopy(list, 0, newList, 1, list.length);

        list = newList;
        list[0] = node;
        
        setPosition(getPosition().union(node.getPosition()));
        return this;
    }
    
    public int size() {
        return list.length;
    }
    
    
    /**
     * Add all elements in other list to this list node.
     * 
     * @param other list which has elements
     * @return this instance for method chaining
     */
    public ListNode addAll(ListNode other) {
        if (other != null && other.size() > 0) {
            int oldSize = grow(other.size());
            
            System.arraycopy(other.list, 0, list, oldSize, other.size());
            
            setPosition(getPosition().union(getLast().getPosition()));
        }
        return this;
    }
    
    /**
     * Add other element to this list
     * 
     * @param other list which has elements
     * @return this instance for method chaining
     */
    public ListNode addAll(Node other) {
        return add(other);
    }
    
    public Node getLast() {
    	return list.length == 0 ? null : list[list.length - 1];
    }
    
    @Override
    public String toString() {
        String string = super.toString();
    	StringBuilder b = new StringBuilder();

    	for (int i = 0; i < list.length; i++) {
    		b.append(list[i]);
            if (i + 1 < list.length) {
                b.append(", ");
            }
    	}
    	return string + ": {" + b.toString() + "}";
    }
    
    public List<Node> childNodes() {
        List<Node> aList = new ArrayList<Node>();
        
        for (int i = 0; i < list.length; i++) {
            aList.add(list[i]);
        }
        return aList;
    }
    
    public Instruction accept(NodeVisitor visitor) {
        throw new RuntimeException("Base class ListNode should never be evaluated");
    }
    
    public Node get(int index) {
        return list[index];
    }
}
