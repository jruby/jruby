/*
 * ScopeNode.java - No description
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

import java.util.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class ScopeNode extends Node implements CallableNode {
    public ScopeNode(RubyIdPointer idTable, CRefNode refValue, Node nextNode) {
        super(Constants.NODE_SCOPE, idTable, refValue, nextNode);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        CRefNode savedCRef = null;
        
        RubyFrame frame = ruby.getRubyFrame();
        frame.setTmp(ruby.getRubyFrame());
        ruby.setRubyFrame(frame);
        
        ruby.getRubyScope().push();
        
        if (getRefValue() != null) {
            savedCRef = ruby.getCRef();
            ruby.setCRef(getRefValue());
            ruby.getRubyFrame().setCbase(getRefValue());
        }
        
        if (getTable() != null) {
            // List tmp = Collections.nCopies(idTable[0].intValue() + 1, ruby.getNil());
            // ShiftableList vars = new ShiftableList(new ArrayList(tmp));
            RubyPointer vars = new RubyPointer(ruby.getNil(), getTable().getId(0).intValue() + 1);
            vars.set(0, this);
            vars.inc();
            ruby.getRubyScope().setLocalVars(vars);
            ruby.getRubyScope().setLocalTbl(getTable());
        } else {
            ruby.getRubyScope().setLocalVars(null);
            ruby.getRubyScope().setLocalTbl(null);
        }
        
        RubyObject result = getNextNode().eval(ruby, self);
        
        ruby.getRubyScope().pop();
        ruby.setRubyFrame(frame.getTmp());
        
        if (savedCRef != null) {
            ruby.setCRef(savedCRef);
        }
        
        return result;
    }
    
    public RubyObject setupModule(Ruby ruby, RubyModule module) {
        // Node node = n;
        
        String file = ruby.getSourceFile();
        int line = ruby.getSourceLine();
        
        // TMP_PROTECT;
        
        RubyFrame frame = ruby.getRubyFrame();
        frame.setTmp(ruby.getRubyFrame());
        ruby.setRubyFrame(frame);
        
        ruby.pushClass();
        ruby.setRubyClass(module);
        ruby.getRubyScope().push();
        RubyVarmap.push(ruby);
        
        if (getTable() != null) {
            /*List tmp = Collections.nCopies(idTable[0].intValue() + 1, ruby.getNil());
            ShiftableList vars = new ShiftableList(new ArrayList(tmp));
            vars.set(0, this);
            vars.shift(1);*/
            RubyPointer vars = new RubyPointer(ruby.getNil(), getTable().getId(0).intValue() + 1);
            vars.set(0, this);
            vars.inc();
            ruby.getRubyScope().setLocalVars(vars);
            ruby.getRubyScope().setLocalTbl(getTable());
        } else {
            ruby.getRubyScope().setLocalVars(null);
            ruby.getRubyScope().setLocalTbl(null);
        }
        
        // +++
        // if (ruby.getCRef() != null) {
            ruby.getCRef().push(module);
        // } else {
        //    ruby.setCRef(new CRefNode(module, null));
        // }
        // ---
        
        ruby.getRubyFrame().setCbase(ruby.getCRef());
        // PUSH_TAG(PROT_NONE);
        
        RubyObject result = null;
        
        // if (( state = EXEC_TAG()) == 0 ) {
        // if (trace_func) {
        //     call_trace_func("class", file, line, ruby_class,
        //                     ruby_frame->last_func, ruby_frame->last_class );
        // }
        result = getNextNode() != null ? getNextNode().eval(ruby, ruby.getRubyClass()) : ruby.getNil();
        // }
        
        // POP_TAG();
        ruby.getCRef().pop();
        RubyVarmap.pop(ruby);
        ruby.getRubyScope().pop();
        ruby.popClass();
        
        ruby.setRubyFrame(frame.getTmp());
        //        if (trace_func) {
        //            call_trace_func("end", file, line, 0, ruby_frame->last_func, ruby_frame->last_class );
        //        }
        // if (state != 0) {
        //     JUMP_TAG(state);
        // }
        
        return result;
    }
    
    public RubyObject call(Ruby ruby, RubyObject recv, RubyId id, RubyObject[] args, boolean noSuper) {
        CRefNode savedCref = ruby.getCRef(); // +++ = null;
        // VALUE[] localVars = null;
        
        RubyPointer argsList = new RubyPointer(args);
        RubyPointer localVarsList = null;
        
        ruby.getRubyScope().push();
        
        if (getRefValue() != null) {
            // savedCref = ruby.getCRef(); s.a.
            ruby.setCRef(getRefValue());
            ruby.getRubyFrame().setCbase(getRefValue());
        }
        if (getTable() != null) {
            
            // ? +++
            // List tmpList = Collections.nCopies(body.nd_tbl()[0].intValue() + 1, getRuby().getNil());
            // ? ---
            // localVarsList = new ShiftableList(new ArrayList(tmpList));
            // localVarsList.set(0, body);
            // localVarsList.shift(1);
            
            localVarsList = new RubyPointer(ruby.getNil(), getTable().getId(0).intValue() + 1);
            localVarsList.set(0, this);
            localVarsList.inc();
            
            ruby.getRubyScope().setLocalVars(localVarsList);
            ruby.getRubyScope().setLocalTbl(getTable());
        } else {
            localVarsList = ruby.getRubyScope().getLocalVars();
            
            ruby.getRubyScope().setLocalVars(null);
            ruby.getRubyScope().setLocalTbl(null);
        }
        
        Node body = getNextNode();
        
        RubyVarmap.push(ruby);
        // PUSH_TAG(PROT_FUNC);
        
        RubyObject result = ruby.getNil();
        
        try {
            Node node = null;
            int i;
            
            if (body.getType() == Constants.NODE_ARGS) {
                node = body;
                body = null;
            } else if (body.getType() == Constants.NODE_BLOCK) {
                node = body.getHeadNode();
                body = body.getNextNode();
            }
            
            if (node != null) {
                if (node.getType() != Constants.NODE_ARGS) {
                    // rb_bug("no argument-node");
                }
                
                i = node.getCount();
                if (i > (args != null ? args.length : 0)) {
                    throw new RubyArgumentException("wrong # of arguments(" + args.length + " for " + i + ")");
                }
                if (node.getRest() == -1) {
                    int opt = i;
                    Node optNode = node.getOptNode();
                    
                    while (optNode != null) {
                        opt++;
                        optNode = optNode.getNextNode();
                    }
                    if (opt < (args != null ? args.length : 0)) {
                        throw new RubyArgumentException("wrong # of arguments(" + args.length + " for " + opt + ")");
                    }
                    
                    ruby.getRubyFrame().setArgs(localVarsList != null ? localVarsList.getPointer(2) : null);
                }
                
                if (localVarsList != null) {
                    if (i > 0) {
                        localVarsList.inc(2);
                        for (int j = 0; j < i; j++ ) {
                            localVarsList.set(j, argsList.get(j));
                        }
                        localVarsList.dec(2);
                    }
                    
                    argsList.inc(i);
                    
                    if (node.getOptNode() != null) {
                        Node optNode = node.getOptNode();
                        
                        while (optNode != null && argsList.size() != 0) {
                            ((AssignableNode)optNode.getHeadNode()).assign(ruby, 
                                                recv, argsList.getRuby(0), true);
                            argsList.inc(1);
                            optNode = optNode.getNextNode();
                        }
                        recv.eval(optNode);
                    }
                    if (node.getRest() >= 0) {
                        RubyArray array = null;
                        if (argsList.size() > 0) {
                            array = RubyArray.m_newArray(ruby, argsList);
                        } else {
                            array = RubyArray.m_newArray(ruby, 0);
                        }
                        localVarsList.set(node.getRest(), array);
                    }
                }
            }
            
            result = recv.eval(body);
        } catch (ReturnException rExcptn) {
        }
        
        RubyVarmap.pop(ruby);
        
        ruby.getRubyScope().pop();
        ruby.setCRef(savedCref);
        
        return result;
    }
}