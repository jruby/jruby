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
    // private RubyPointer vars = null;
    // private RubyPointer localVarsList = null;
    
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
    
    public RubyObject call(Ruby ruby, RubyObject recv, RubyId id, RubyPointer args, boolean noSuper) {
        CRefNode savedCref = null; // +++ = null;
        
        // RubyPointer argsList = new RubyPointer(args);
        
        RubyPointer localVarsList = null;
        
        ruby.getRubyScope().push();
        
        if (getRefValue() != null) {
            savedCref = ruby.getCRef(); // s.a.
            ruby.setCRef(getRefValue());
            ruby.getRubyFrame().setCbase(getRefValue());
        }
        
        if (getTable() != null) {
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
        
        Node callBody = getNextNode();
        Node callNode = null;
        if (callBody.getType() == Constants.NODE_ARGS) {
            callNode = callBody;
            callBody = null;
        } else if (callBody.getType() == Constants.NODE_BLOCK) {
            callNode = callBody.getHeadNode();
            callBody = callBody.getNextNode();
        }
        
        RubyVarmap.push(ruby);
        // PUSH_TAG(PROT_FUNC);
        
        RubyObject result = ruby.getNil();
        
        try {
            if (callNode != null) {
                //if (call_node.getType() != Constants.NODE_ARGS) {
                // rb_bug("no argument-node");
                //}
                
                int i = callNode.getCount();
                if (i > (args != null ? args.size() : 0)) {
                    throw new RubyArgumentException(ruby, "wrong # of arguments(" + args.size() + " for " + i + ")");
                }
                if (callNode.getRest() == -1) {
                    int opt = i;
                    Node optNode = callNode.getOptNode();
                    
                    while (optNode != null) {
                        opt++;
                        optNode = optNode.getNextNode();
                    }
                    if (opt < (args != null ? args.size() : 0)) {
                        throw new RubyArgumentException(ruby, "wrong # of arguments(" + args.size() + " for " + opt + ")");
                    }
                    
                    ruby.getRubyFrame().setArgs(localVarsList != null ? localVarsList.getPointer(2) : null);
                }
                
                if (localVarsList != null) {
                    if (i > 0) {
                        localVarsList.inc(2);
                        for (int j = 0; j < i; j++ ) {
                            localVarsList.set(j, args.get(j));
                        }
                        localVarsList.dec(2);
                    }
                    
                    args.inc(i);
                    
                    if (callNode.getOptNode() != null) {
                        Node optNode = callNode.getOptNode();
                        
                        while (optNode != null && args.size() != 0) {
                            ((AssignableNode)optNode.getHeadNode()).assign(ruby,
                            recv, args.getRuby(0), true);
                            args.inc(1);
                            optNode = optNode.getNextNode();
                        }
                        recv.eval(optNode);
                    }
                    if (callNode.getRest() >= 0) {
                        RubyArray array = null;
                        if (args.size() > 0) {
                            array = RubyArray.m_newArray(ruby, args);
                        } else {
                            array = RubyArray.m_newArray(ruby, 0);
                        }
                        localVarsList.set(callNode.getRest(), array);
                    }
                }
            }
            
            result = recv.eval(callBody);
        } catch (ReturnException rExcptn) {
        }
        
        RubyVarmap.pop(ruby);
        
        ruby.getRubyScope().pop();
        
        if (savedCref != null) {
            ruby.setCRef(savedCref);
        }
        
        return result;
    }
}