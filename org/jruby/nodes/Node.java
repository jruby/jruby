/*
 * Node.java - No description
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
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
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class Node {
    public static final Node ONE = new Node(-1);
    public static final Node MINUS_ONE = new Node(-1);
    
    private int line;
    private String file;
    private int type;
    
    private Object u1;
    private Object u2;
    private Object u3;
    
    protected Node(int type) {
        this(type, null, null, null);
    }
    
    protected Node(int type, int u1, Object u2, Object u3) {
        this(type, new Integer(u1), u2, u3);
    }
    
    protected Node(int type, Object u1, int u2, Object u3) {
        this(type, u1, new Integer(u2), u3);
    }
    
    protected Node(int type, Object u1, Object u2, int u3) {
        this(type, u1, u2, new Integer(u3));
    }

    protected Node(int type, Object u1, int u2, int u3) {
        this(type, u1, new Integer(u2), new Integer(u3));
    }

    protected Node(int type, Object u1, Object u2, Object u3) {
        this.type = type;
        
        this.u1 = u1;
        this.u2 = u2;
        this.u3 = u3;
    }
    
    public int getLine() {
        return line;
    }
    
    public void setLine(int line) {
        this.line = line;
    }
    
    public String getFile() {
        return file;
    }
    
    public void setFile(String file) {
        this.file = file;
    }
    
    public int getType() {
        return type;
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        throw new UnsupportedOperationException();
    }
    
    /** copy_node_scope
     *
     */
    public Node copyNodeScope(CRefNode refValue) {
        Node copy = new ScopeNode(null, refValue.cloneCRefNode(), getNextNode());
        
        if (getTable() != null) {
            RubyIdPointer newTable = new RubyIdPointer(getTable().getId(0).intValue() + 1);
            newTable.set(getTable(), getTable().getId(0).intValue() + 1);
            
            copy = new ScopeNode(newTable, refValue.cloneCRefNode(), getNextNode());
        }
        
        return copy;
    }
    
    // The getter and setter methods
    
    public Node getHeadNode() {
        return (Node)u1;
    }
    
    public void setHeadNode(Node headNode) {
        u1 = headNode;
    }
    
    public int getALength() {
        return u2 == null ? 0 : ((Integer)u2).intValue();
    }
    
    public void setALength(int alength) {
        u2 = new Integer(alength);
    }
    
    public Node getNextNode() {
        return (Node)u3;
    }
    
    public void setNextNode(Node nextNode) {
        u3 = nextNode;
    }
    
    public Node getConditionNode() {
        return (Node)u1;
    }
    
    public Node getBodyNode() {
        return (Node)u2;
    }

    public Node getElseNode() {
        return (Node)u3;
    }
    
    public RubyObject getOrigin() {
        return (RubyObject)u3;
    }

    public Node getResqNode() {
        return (Node)u2;
    }

    public Node getEnsureNode() {
        return (Node)u3;
    }
    
    public Node getFirstNode() {
        return (Node)u1;
    }

    public Node getSecondNode() {
        return (Node)u2;
    }

    public Node getSttsNode() {
        return (Node)u1;
    }

    public RubyGlobalEntry getEntry() {
        return (RubyGlobalEntry)u3;
    }

    public RubyId getVId() {
        return (RubyId)u1;
    }

    public int getCFlag() {
        return u2 == null ? 0 : ((Integer)u2).intValue();
    }
    
    public void setCFlag(int cflag) {
        u2 = cflag == 0 ? null : new Integer(cflag);
    }
    
    public RubyObject getCValue() {
        return (RubyObject)u3;
    }

    public int getCount() {
        return u3 == null ? 0 : ((Integer)u3).intValue();
    }
    
    public RubyIdPointer getTable() {
        return (RubyIdPointer)u1;
    }

    public void setTable(RubyIdPointer newTable) {
        u1 = newTable;
    }

    public Node getVarNode() {
        return (Node)u1;
    }
    
    public Node getIBodyNode() {
        return (Node)u2;
    }

    public Node getIterNode() {
        return (Node)u3;
    }
    
    public void setIterNode(Node newIterNode) {
        u3 = newIterNode;
    }
    
    public Node getValueNode() {
        return (Node)u2;
    }

    public void setValueNode(Node newValueNode) {
        u2 = newValueNode;
    }

    public RubyId getAId() {
        return (RubyId)u3;
    }
    
    public void setAId(RubyId newAid) {
        u3 = newAid;
    }
    
    public RubyObject getLiteral() {
        return (RubyObject)u1;
    }
    
    public void setLiteral(RubyObject newLiteral) {
        u1 = newLiteral;
    }
    
    public Node getFrmlNode() {
        return (Node)u1;
    }
    
    public int getRest() {
        return u2 == null ? 0 : ((Integer)u2).intValue();
    }
    
    public Node getOptNode() {
        return (Node)u1;
    }
    
    public Node getRecvNode() {
        return (Node)u1;
    }
    
    public RubyId getMId() {
        return (RubyId)u2;
    }
    
    public Node getArgsNode() {
        return (Node)u3;
    }
    
    public void setArgsNode(Node newArgsNode) {
        u3 = newArgsNode;
    }
    
    public int getNoex() {
        return u1 == null ? 0 : ((Integer)u1).intValue();
    }
    
    public void setNoex(int newNoex) {
        u1 = newNoex == 0 ? null : new Integer(newNoex);
    }
    
    public Node getDefnNode() {
        return (Node)u3;
    }
    
    public RubyId getOldId() {
        return (RubyId)u1;
    }
    
    public RubyId getNewId() {
        return (RubyId)u2;
    }
    
    public Callback getCallbackMethod() {
        return (Callback)u1;
    }
    
    /*  This method is not needed in JRuby
     *
     *@deprecated
     */
    public int getArgsCount() {
        return u2 == null ? 0 : ((Integer)u2).intValue();
    }

    public RubyId getClassNameId() {
        return (RubyId)u1;
    }
    
    public Node getSuperNode() {
        return (Node)u3;
    }

    /* 
     * ?
     */ 
    public RubyId getModlId() {
        return (RubyId)u1;
    }

    /*
     * used by CRefNode
     */
    public RubyObject getClassValue() {
        return (RubyObject)u1;
    }
    
    /*
     * used by CRefNode
     */
    public void setClassValue(RubyObject newClassValue) {
        u1 = newClassValue;
    }

    public Node getBeginNode() {
        return (Node)u1;
    }
    
    public void setBeginNode(Node newBeginNode) {
        u1 = newBeginNode;
    }
    
    public Node getEndNode() {
        return (Node)u2;
    }

    public void setEndNode(Node newEndNode) {
        u2 = newEndNode;
    }
    /* ?
     *
     *@deprecated
     */
    public int getState() {
        return u3 == null ? 0 : ((Integer)u3).intValue();
    }

    /** nd_rval -> getRefValue
     * RubyObject -> CRefNode
     */
    public CRefNode getRefValue() {
        return (CRefNode)u2;
    }
    
    public void setRefValue(CRefNode newRefValue) {
        u2 = newRefValue;
    }
    
    public int getNth() {
        return u2 == null ? 0 : ((Integer)u2).intValue();
    }
    
    public void setNth(int newNth) {
        u2 = newNth == 0 ? null : new Integer(newNth);
    }
    
    public RubyId getTagId() {
        return (RubyId)u1;
    }
    
    public RubyObject getTValue() {
        return (RubyObject)u2;
    }
}