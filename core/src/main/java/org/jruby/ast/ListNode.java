/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * All Nodes which have a list representation inherit this.  This is also used
 * as generic container for additional information that is not directly evaluated.
 * In particular, f_arg production rule uses this to capture arg information for
 * the editor projects who want position info saved.
 */
public class ListNode extends Node {
    private List<Node> list;

    /**
     * Create a new ListNode.
     * 
     * @param position where list is
     * @param firstNode first element of the list
     */
    public ListNode(ISourcePosition position, Node firstNode) {
        super(position, firstNode != null && firstNode.containsVariableAssignment);
        
        list = new ArrayList<>(4);
        list.add(firstNode);
    }
    
    public ListNode(ISourcePosition position) {
        super(position, false);
        
        list = new ArrayList<>(0);
    }

    public NodeType getNodeType() {
        return NodeType.LISTNODE;
    }
    
    public ListNode add(Node node) {
        // Ruby Grammar productions return plenty of nulls.
        if (node == null || node == NilImplicitNode.NIL) {
            list.add(NilImplicitNode.NIL);

            return this;
        }

        if (node.containsVariableAssignment()) containsVariableAssignment = true;
        list.add(node);

        if (getPosition() == null) setPosition(node.getPosition());

        return this;
    }
    
    public ListNode prepend(Node node) {
        // Ruby Grammar productions return plenty of nulls.
        if (node == null) return this;

        if (node.containsVariableAssignment()) containsVariableAssignment = true;
        list.add(0, node);
        
        setPosition(node.getPosition());
        return this;
    }
    
    public int size() {
        return list.size();
    }
    
    
    /**
     * Add all elements in other list to this list node.
     * 
     * @param other list which has elements
     * @return this instance for method chaining
     */
    public ListNode addAll(ListNode other) {
        if (other != null && other.size() > 0) {
            if (other.containsVariableAssignment()) containsVariableAssignment = true;
            list.addAll(other.list);

            if (getPosition() == null) setPosition(other.getPosition());
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
    	return list.size() == 0 ? null : list.get(list.size() - 1);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public List<Node> childNodes() {
        return list;
    }
    
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitListNode(this);
    }
    
    public Node get(int idx) {
        return list.get(idx);
    }
}
