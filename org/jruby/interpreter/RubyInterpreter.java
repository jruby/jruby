/*
 * RubyInterpreter.java - No description
 * Created on 23. Juli 2001, 19:27
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby.interpreter;

import java.util.*;

import org.jruby.*;
import org.jruby.core.*;
import org.jruby.exceptions.*;
import org.jruby.original.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyInterpreter implements node_type, Scope {
    private Ruby ruby;
    
    private Iter rubyIter = new Iter(); // HACK 
    
    // C
    
    // private RubyModule ruby_class;
    private RubyModule ruby_cbase;
    private RubyModule ruby_wrapper;
    
    private int actMethodScope;
    
    private RubyBlock rubyBlock;
        
    public RubyInterpreter(Ruby ruby) {
        this.ruby = ruby;
        
        // ruby.setRubyClass(ruby.getClasses().getObjectClass());
        // ruby_cbase = null;
        rubyBlock = new RubyBlock(ruby);
    }
    
    /** rb_eval
     *
     */    
    public RubyObject eval(RubyObject self, NODE n) {
        NODE node = n;
        
        RubyObject cond = null;
        RubyObject[] args = null;
        RubyObject value = null;
        RubyObject result = null;
        RubyModule rubyClass = null;
        // int state;
        
        // RubyOriginalMethods rom = getRuby().getOriginalMethods();
        
        while (true) {
            
            if (node == null) {
                return ruby.getNil();
            }
            
            switch (node.nd_type()) {
                case NODE_BLOCK:
                    while (node.nd_next() != null) {
                        eval(self, node.nd_head());
                        node = node.nd_next();
                    }
                    node = node.nd_head();
                    break;
                    
                case NODE_POSTEXE:
                    // rb_f_END();
                    node.nd_set_type(NODE_NIL); /* exec just once */
                    return ruby.getNil();
                    
                /* begin .. end without clauses */
                case NODE_BEGIN:
                    node = node.nd_body();
                    break;
                    
                /* nodes for speed-up(default match) */
                case NODE_MATCH:
                    return ((RubyRegexp)node.nd_head().nd_lit()).m_match2();
                    
                /* nodes for speed-up(literal match) */
                case NODE_MATCH2:
                    return ((RubyRegexp)eval(self, node.nd_recv())).m_match(eval(self, node.nd_value()));
                    
                /* nodes for speed-up(literal match) */
                case NODE_MATCH3:
                    RubyObject r = eval(self, node.nd_recv());
                    RubyObject l = eval(self, node.nd_value());
                    if (l instanceof RubyString) {
                        return ((RubyRegexp)r).m_match(l);
                    } else {
                        return l.funcall(ruby.intern("=~"), r);
                    }
                    
                /* node for speed-up(top-level loop for -n/-p) */
                case NODE_OPT_N:
                    while (true) {
                        try {
                            // while (!rb_gets().isNil() false) {
                            // HACK +++
                            if (true) {
                            // HACK ---
                                try {
                                    eval(self, node.nd_body());
                                } catch (RedoException rExcptn) {
                                }
                            }
                            break;
                        } catch (NextException nExcptn) {
                        } catch (BreakException bExcptn) {
                            break;
                        }
                    }
                    return ruby.getNil();
                    
                case NODE_SELF:
                    return self;
                    
                case NODE_NIL:
                    return ruby.getNil();
                    
                case NODE_TRUE:
                    return ruby.getTrue();
                    
                case NODE_FALSE:
                    return ruby.getFalse();
                    
                case NODE_IF:
                    //                ruby_sourceline = node.nd_line();
                    cond = eval(self, node.nd_cond());
                    if (cond.isTrue()) {
                        node = node.nd_body();
                    } else {
                        node = node.nd_else();
                    }
                    break;
                    
                case NODE_WHEN:
                    while (node != null) {
                        NODE tag;
                        
                        if (node.nd_type() != NODE_WHEN) {
                            break;
                        }
                        
                        tag = node.nd_head();
                        while (tag != null) {
/*                        if (trace_func) {
                            call_trace_func("line", tag->nd_file, nd_line(tag), self,
                                    ruby_frame->last_func,
                                    ruby_frame->last_class);
                        }*/
                            //                        ruby_sourcefile = tag.nd_file;
                            //                        ruby_sourceline = tag.nd_line();
                            
                            if (tag.nd_head().nd_type() == NODE_WHEN) {
                                RubyObject obj = eval(self, tag.nd_head().nd_head());
                                
                                if (!(obj instanceof RubyArray)) {
                                    obj = RubyArray.m_newArray(ruby, obj);
                                }
                                
                                for (int i = 0; i < ((RubyArray)obj).length(); i++) {
                                    if (((RubyArray)obj).entry(i).isTrue()) {
                                        node = node.nd_body();
                                        break;
                                    }
                                }
                                tag = tag.nd_next();
                                continue;
                            }
                            if (eval(self, tag.nd_head()).isTrue()) {
                                node = node.nd_body();
                                break;
                            }
                            tag = tag.nd_next();
                        }
                        node = node.nd_next();
                    }
                    return ruby.getNil();
                    
                case NODE_CASE:
                    
                    RubyObject obj = eval(self, node.nd_head());
                    node = node.nd_body();
                    while (node != null) {
                        NODE tag;
                        
                        if (node.nd_type() != NODE_WHEN) {
                            break;
                        }
                        tag = node.nd_head();
                        while (tag != null) {
/*                        if (trace_func) {
                            call_trace_func("line", tag->nd_file, nd_line(tag), self,
                                                ruby_frame->last_func,
                                                ruby_frame->last_class);
                        }*/
                            
                            ruby.setSourceFile(tag.nd_file);
                            ruby.setSourceLine(tag.nd_line());
                            
                            if (tag.nd_head().nd_type() == NODE_WHEN) {
                                RubyObject obj2 = eval(self, tag.nd_head().nd_head());
                                
                                if (!(obj2 instanceof RubyArray)) {
                                    obj2 = RubyArray.m_newArray(ruby, obj2);
                                }
                                for (int i = 0; i < ((RubyArray)obj).length(); i++) {
                                    RubyObject eqq = ((RubyArray)obj2).entry(i).funcall(ruby.intern("==="), obj);
                                    if (eqq.isTrue()) {
                                        node = node.nd_body();
                                        break;
                                    }
                                }
                                tag = tag.nd_next();
                                continue;
                            }
                            if (eval(self, tag.nd_head()).funcall(ruby.intern("==="), obj).isTrue()) {
                                node = node.nd_body();
                                break;
                            }
                            tag = tag.nd_next();
                        }
                        node = node.nd_next();
                    }
                    return ruby.getNil();
                    
                case NODE_WHILE:
                    while (eval(self, node.nd_cond()).isTrue()) {
                        while (true) {
                            try {
                                eval(self, node.nd_body());
                                break;
                            } catch (RedoException rExcptn) {
                            } catch (NextException nExcptn) {
                                break;
                            } catch (BreakException bExcptn) {
                                return ruby.getNil();
                            }
                        }
                    }
                    
                    return ruby.getNil();
                    
                case NODE_UNTIL:
                    while (eval(self, node.nd_cond()).isFalse()) {
                        while (true) {
                            try {
                                eval(self, node.nd_body());
                                break;
                            } catch (RedoException rExcptn) {
                            } catch (NextException nExcptn) {
                                break;
                            } catch (BreakException bExcptn) {
                                return ruby.getNil();
                            }
                        }
                    }
                    return ruby.getNil();
                    
                case NODE_BLOCK_PASS:
                    //return block_pass(node);
                    return null;
                    
                case NODE_ITER:
                case NODE_FOR:
                    rubyBlock.push(node.nd_var(), node.nd_body(), self);
                    rubyIter.push(Iter.ITER_PRE);
                    while (true) {
                        try {
                            if (node.nd_type() == NODE_ITER) {
                                result = eval(self, node.nd_iter());
                            } else {
                                // String file = 
                                // int line = 
                                
                                rubyBlock.flags &= ~RubyBlock.BLOCK_D_SCOPE;
                                
                                RubyBlock tmpBlock = beginCallargs();
                                RubyObject recv = eval(self, node.nd_iter());
                                endCallArgs(tmpBlock);
                                
                                // = file;
                                // = line;
                                result = recv.getRubyClass().call(recv, ruby.intern("each"), null, 0);
                            }
                            break;
                        } catch (RetryException rExcptn) {
                        } catch (ReturnException rExcptn) {
                            result = rExcptn.getReturnValue();
                            break;
                        } catch (BreakException bExcptn) {
                            result = ruby.getNil();
                            break;
                        }
                    }
                    rubyIter.pop();
                    rubyBlock.pop();
                    return result;
                    
                case NODE_BREAK:
                    throw new BreakException();
                    
                case NODE_NEXT:
                    throw new NextException();
                    
                case NODE_REDO:
                    throw new RedoException();
                    
                case NODE_RETRY:
                    throw new RetryException();
                    
                case NODE_RESTARGS:
                    result = eval(self, node.nd_head());
                    if (!(result instanceof RubyArray)) {
                        result = obj = RubyArray.m_newArray(ruby, result);
                    }
                    return result;
                    
                case NODE_YIELD:
                    if (node.nd_stts() != null) {
                        result = eval(self, node.nd_stts());
                        if (node.nd_stts().nd_type() == NODE_RESTARGS && 
                            ((RubyArray)result).length() == 1) {

                            result = ((RubyArray)result).entry(0);
                        }
                    } else {
                        result = ruby.getNil();
                    }
                    return yield0(result, null, null, false);
                    
                case NODE_RESCUE:
/*            retry_entry:
        {
            volatile VALUE e_info = ruby_errinfo;
 
            PUSH_TAG(PROT_NONE);
            if ((state = EXEC_TAG()) == 0) {
                result = rb_eval(self, node->nd_head);
            }
            POP_TAG();
            if (state == TAG_RAISE) {
                NODE * volatile resq = node->nd_resq;
 
                ruby_sourceline = nd_line(node);
                while (resq) {
                    if (handle_rescue(self, resq)) {
                        state = 0;
                        PUSH_TAG(PROT_NONE);
                        if ((state = EXEC_TAG()) == 0) {
                            result = rb_eval(self, resq->nd_body);
                        }
                        POP_TAG();
                        if (state == TAG_RETRY) {
                            state = 0;
                            ruby_errinfo = Qnil;
                            goto retry_entry;
                        }
                        if (state != TAG_RAISE) {
                            ruby_errinfo = e_info;
                        }
                        break;
                    }
                    resq = resq->nd_head; /* next rescue */
                    /**		}
                     * }
                     * else if (node->nd_else) { /* else clause given */
/*		if (!state) {	/* no exception raised */
/*		    result = rb_eval(self, node->nd_else);
                }
            }
            if (state) JUMP_TAG(state);
        }
        break;*/
                    
                case NODE_ENSURE:
/*                PUSH_TAG(PROT_NONE);
                if ((state = EXEC_TAG()) == 0) {
                    result = eval(node.nd_head());
                }
                POP_TAG();
                if (node.nd_ensr()) {
                    VALUE retval = prot_tag->retval; /* save retval */
/*                    VALUE errinfo = ruby_errinfo;
 
            rb_eval(self, node->nd_ensr);
            return_value(retval);
            ruby_errinfo = errinfo;
        }
        if (state) JUMP_TAG(state);
        break;*/
                    
                case NODE_AND:
                    cond = eval(self, node.nd_1st());
                    if (cond.isFalse()) {
                        return cond;
                    }
                    node = node.nd_2nd();
                    break;
                    
                case NODE_OR:
                    cond = eval(self, node.nd_1st());
                    if (cond.isTrue()) {
                        return cond;
                    }
                    node = node.nd_2nd();
                    break;
                    
                case NODE_NOT:
                    return RubyBoolean.m_newBoolean(ruby, eval(self, node.nd_1st()).isFalse());
                    
                case NODE_DOT2:
                case NODE_DOT3:
                    result = RubyRange.m_newRange(ruby, eval(self, node.nd_beg()),
                                                             eval(self, node.nd_end()),
                                                             node.nd_type() == NODE_DOT3);
                    if (node.nd_state() != 0) {
                        return result;
                    }
                    
                    if (node.nd_beg().nd_type() == NODE_LIT && (node.nd_beg().nd_lit() instanceof RubyFixnum) &&
                        node.nd_end().nd_type() == NODE_LIT && (node.nd_end().nd_lit() instanceof RubyFixnum)) {
                        
                        node.nd_set_type(NODE_LIT);
                        node.nd_lit(result);
                    } else {
                        node.nd_state(1L);
                    }
                    return result;
                    
                case NODE_FLIP2:		/* like AWK */
                    /*if (ruby_scope->local_vars == 0) {
                        rb_bug("unexpected local variable");
                    }*/
                    
                    if (ruby.getRubyScope().getLocalVars(node.nd_cnt()).isFalse()) {
                        if (eval(self, node.nd_beg()).isTrue()) {
                            ruby.getRubyScope().setLocalVars(node.nd_cnt(), 
                                eval(self, node.nd_end()).isTrue() ? ruby.getFalse() : ruby.getTrue());
                            result = ruby.getTrue();
                        } else {
                            result = ruby.getFalse();
                        }
                    } else {
                        if (eval(self, node.nd_end()).isTrue()) {
                            ruby.getRubyScope().setLocalVars(node.nd_cnt(), ruby.getFalse());
                        }
                        result = ruby.getTrue();
                    }
                    return result;
                    
                case NODE_FLIP3:		/* like SED */
                    /*if (ruby_scope->local_vars == 0) {
                        rb_bug("unexpected local variable");
                    }*/
                    
                    if (ruby.getRubyScope().getLocalVars(node.nd_cnt()).isFalse()) {
                        result = eval(self, node.nd_beg()).isTrue() ? ruby.getFalse() : ruby.getTrue();
                        ruby.getRubyScope().setLocalVars(node.nd_cnt(), result);
                    } else {
                        if (eval(self, node.nd_end()).isTrue()) {
                            ruby.getRubyScope().setLocalVars(node.nd_cnt(), ruby.getFalse());
                        }
                        result = ruby.getTrue();
                    }
                    return result;
                    
                case NODE_RETURN:
                    if (node.nd_stts() != null) {
                        result = eval(self, node.nd_stts());
                    } else {
                        result = ruby.getNil();
                    }
                    throw new ReturnException(result);
                    
                case NODE_ARGSCAT:
                    return ((RubyArray)eval(self, node.nd_head())).m_concat(eval(self, node.nd_body()));
                    
                case NODE_ARGSPUSH:
                    return ((RubyArray)eval(self, node.nd_head()).m_dup()).push(eval(self, node.nd_body()));
                    
                case NODE_CALL:
                    //                TMP_PROTECT;
                    
                    RubyBlock tmpBlock = beginCallargs();
                                
                    RubyObject recv = eval(self, node.nd_recv());
                    args = setupArgs(self, node.nd_args());
                    
                    endCallArgs(tmpBlock);
                    
                    return recv.getRubyClass().call(recv, node.nd_mid(), args, 0);
                    
                case NODE_FCALL:
                    //                TMP_PROTECT;
                    
                    tmpBlock = beginCallargs();
                    
                    args = setupArgs(self, node.nd_args());
                    
                    endCallArgs(tmpBlock);
                    
                    return self.getRubyClass().call(self, node.nd_mid(), args, 1);
                    
                case NODE_VCALL:
                    return self.getRubyClass().call(self, node.nd_mid(), null, 2);
                    
                case NODE_SUPER:
                case NODE_ZSUPER:
                    //                TMP_PROTECT;
                    
                    if (ruby.getRubyFrame().getLastClass() == null) {
                        throw new RubyNameException("superclass method '" + ruby.getRubyFrame().getLastFunc().toName() + "' disabled");
                    }
                    if (node.nd_type() == NODE_ZSUPER) {
                        List argsList = ruby.getRubyFrame().getArgs();
                        args = (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]);
                    } else {
                        tmpBlock = beginCallargs();
                        args = setupArgs(self, node.nd_args());
                        endCallArgs(tmpBlock);
                    }
 
                    rubyIter.push(rubyIter.getIter() != Iter.ITER_NOT ? Iter.ITER_PRE : Iter.ITER_NOT);
                    result = ruby.getRubyFrame().getLastClass().getSuperClass().call(
                             ruby.getRubyFrame().getSelf(), ruby.getRubyFrame().getLastFunc(), args, 3);
                    rubyIter.pop();
                    
                    return result;
                    
                case NODE_SCOPE:
                    NODE saved_cref = null;
                    
                    RubyFrame frame = ruby.getRubyFrame();
                    frame.setTmp(ruby.getRubyFrame());
                    ruby.setRubyFrame(frame);
                    
                    ruby.getRubyScope().push();
                    
                    if (node.nd_rval() != null) {
                        saved_cref = ruby.getRubyCRef();
                        ruby.setRubyCRef((NODE)node.nd_rval());
                        ruby.getRubyFrame().setCbase(node.nd_rval());
                    }
                    
                    if (node.nd_tbl() != null) {
                        List tmp = Collections.nCopies(node.nd_tbl()[0].intValue() + 1, ruby.getNil());
                        ShiftableList vars = new ShiftableList(new ArrayList(tmp));
                        vars.set(0, (VALUE)node);
                        vars.shift(1);
                        ruby.getRubyScope().setLocalVars(vars);
                        ruby.getRubyScope().setLocalTbl(node.nd_tbl());
                    } else {
                        ruby.getRubyScope().setLocalVars(null);
                        ruby.getRubyScope().setLocalTbl(null);
                    }
                    
                    result = eval(self, node.nd_next());
                    
                    ruby.getRubyScope().pop();
                    ruby.setRubyFrame(frame.getTmp());
                    
                    if (saved_cref != null) {
                        ruby.setRubyCRef(saved_cref);
                    }
                    
                    return result;

                case NODE_OP_ASGN1:
                    //                TMP_PROTECT;
                    
                    recv = eval(self, node.nd_recv());
                    NODE rval = node.nd_args().nd_head();
                    
                    args = setupArgs(self, node.nd_args().nd_next());
                    
                    ArrayList argsList = new ArrayList(Arrays.asList(args));
                    argsList.remove(args.length - 1);                    
                    RubyObject val = recv.funcall(ruby.intern("[]"), (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]));
                    
                    switch (node.nd_mid().intValue()) {
                        case 0: /* OR */
                            if (val.isTrue()) {
                                return val;
                            }
                            val = eval(self, rval);
                            break;
                        case 1: /* AND */
                            if (val.isFalse()) {
                                return val;
                            }
                            val = eval(self, rval);
                            break;
                        default:
                            val = val.funcall(node.nd_mid(), eval(self, rval));
                    }
                    args[args.length - 1] = val;
                    return recv.funcall(ruby.intern("[]="), args);
                    
                case NODE_OP_ASGN2:
                    RubyId id = node.nd_next().nd_vid();
                    
                    recv = eval(self, node.nd_recv());
                    val = recv.funcall(id, (RubyObject[])null);
                    
                    switch (node.nd_next().nd_mid().intValue()) {
                        case 0: /* OR */
                            if (val.isTrue()) {
                                return val;
                            }
                            val = eval(self, node.nd_value());
                            break;
                        case 1: /* AND */
                            if (val.isFalse()) {
                                return val;
                            }
                            val = eval(self, node.nd_value());
                            break;
                        default:
                            val = val.funcall(node.nd_mid(), eval(self, node.nd_value()));
                    }
                    
                    // HACK +++
                    val = recv.funcall(node.nd_next().nd_aid(), val);
                    // HACK ---
                    
                    return val;
                    
                case NODE_OP_ASGN_AND:
                    cond = eval(self, node.nd_head());
                    
                    if (cond.isFalse()) {
                        return cond;
                    }
                    node = node.nd_value();
                    
                    break;
                    
                case NODE_OP_ASGN_OR:
                    cond = eval(self, node.nd_head());
                    
                    if ((node.nd_aid() != null && !self.isInstanceVarDefined(node.nd_aid())) || cond.isFalse()) {
                        node = node.nd_value();
                        break;
                    }
                    return cond;
                    
                case NODE_MASGN:
                    return massign(self, node, eval(self, node.nd_value()), false);
                    
                case NODE_LASGN:
                    // if (ruby.ruby_scope.local_vars == null) {
                    //     rb_bug("unexpected local variable assignment");
                    // }
                    result = eval(self, node.nd_value());
                    ruby.getRubyScope().setLocalVars(node.nd_cnt(), result);
                    return result;
                    
                case NODE_DASGN:
                    result = eval(self, node.nd_value());
                    RubyVarmap.assign(ruby, node.nd_vid(), result);
                    return result;
                    
                case NODE_DASGN_CURR:
                    result = eval(self, node.nd_value());
                    RubyVarmap.assignCurrent(ruby, node.nd_vid(), result);
                    return result;
                    
                case NODE_GASGN:
                    result = eval(self, node.nd_value());
                    ((RubyGlobalEntry)node.nd_entry()).set(result);
                    return result;
                    
                case NODE_IASGN:
                    result = eval(self, node.nd_value());
                    self.setInstanceVar(node.nd_vid(), result);
                    return result;
                    
                case NODE_CDECL:
                    if (ruby.getRubyClass() == null) {
                        throw new RubyTypeException("no class/module to define constant");
                    }
                    result = eval(self, node.nd_value());
                    ruby.getRubyClass().setConstant(node.nd_vid(), result);
                    return result;
                    
                case NODE_CVDECL:
                    if (ruby_cbase == null) {
                        throw new RubyTypeException("no class/module to define class variable");
                    }
                    result = eval(self, node.nd_value());
                    if (ruby_cbase.isSingleton()) {
                        ruby_cbase.getInstanceVar("__attached__").getClassVarSingleton().declareClassVar(node.nd_vid(), result);
                        return result;
                    }
                    ruby_cbase.declareClassVar(node.nd_vid(), result);
                    return result;
                    
                case NODE_CVASGN:
                    result = eval(self, node.nd_value());
                    self.getClassVarSingleton().setClassVar(node.nd_vid(), result);
                    return result;
                    
                case NODE_LVAR:
                    //if (getRuby().ruby_scope.local_vars == null) {
                    //     rb_bug("unexpected local variable");
                    // }
                    return ruby.getRubyScope().getLocalVars(node.nd_cnt());
                    
                case NODE_DVAR:
                    return ruby.getDynamicVars().getRef(node.nd_vid());
                    
                case NODE_GVAR:
                    return node.nd_entry().get();
                    
                case NODE_IVAR:
                    return self.getInstanceVar(node.nd_vid());
                    
                case NODE_CONST:
                    return getConstant((NODE)ruby.getRubyFrame().getCbase(), node.nd_vid(), self);
                    
                case NODE_CVAR:     /* normal method */
                    if (ruby_cbase == null) {
                        return self.getRubyClass().getClassVar(node.nd_vid());
                    }
                    if (!ruby_cbase.isSingleton()) {
                        return ruby_cbase.getClassVar(node.nd_vid());
                    }
                    
                    return ruby_cbase.getInstanceVar("__attached__").getClassVarSingleton().getClassVar(node.nd_vid());
                    
                case NODE_CVAR2:		/* singleton method */
                    return self.getClassVarSingleton().getClassVar(node.nd_vid());
                    
                case NODE_BLOCK_ARG:
                    if (ruby.getRubyScope().getLocalVars() == null) {
                        throw new RuntimeException("BUG: unexpected block argument");
                    }
                    if (isBlockGiven()) {
                        result = ruby.getNil(); // Create Proc object
                        ruby.getRubyScope().setLocalVars(node.nd_cnt(), result);
                        return result;
                    } else {
                        return ruby.getNil();
                    }
                    
                case NODE_COLON2:
                    value = eval(self, node.nd_head());
                    if (value instanceof RubyModule) {
                        return ((RubyModule)value).getConstant(node.nd_mid());
                    } else {
                        return value.funcall((RubyId)node.nd_mid());
                    }
                    
                case NODE_COLON3:
                    return ruby.getClasses().getObjectClass().getConstant(node.nd_mid());
                    
                case NODE_NTH_REF:
                    return RubyRegexp.m_nth_match(node.nd_nth(), ruby.getBackRef());
                    
                case NODE_BACK_REF:
                    switch ((char)node.nd_nth()) {
                        case '&':
                            return RubyRegexp.m_last_match(ruby.getBackRef());
                            
                        case '`':
                            return RubyRegexp.m_match_pre(ruby.getBackRef());
                            
                        case '\'':
                            return RubyRegexp.m_match_post(ruby.getBackRef());
                            
                        case '+':
                            return RubyRegexp.m_match_last(ruby.getBackRef());
                            
                        default:
                            throw new RubyBugException("unexpected back-ref");
                    }
                    
                case NODE_HASH:
                    RubyHash hash = RubyHash.m_newHash(ruby);
                    
                    NODE list = node.nd_head();
                    while(list != null) {
                        RubyObject key = eval(self, list.nd_head());
                        list = list.nd_next();
                        if (list == null) {
                            // HACK +++
                            throw new RubyArgumentException("odd number list for Hash");
                            // HACK ---
                        }
                        value = eval(self, list.nd_head());
                        hash.m_aset(key, value);
                        
                        list = list.nd_next();
                    }
                    return hash;
                    
                case NODE_ZARRAY:		/* zero length list */
                    return RubyArray.m_newArray(ruby, 0);
                    
                case NODE_ARRAY:
                    ArrayList ary = new ArrayList(node.nd_alen());
                    for (; node != null ; node = node.nd_next()) {
                        ary.add(eval(self, node.nd_head()));
                    }
                    return RubyArray.m_newArray(ruby, ary);
                    
                case NODE_STR:
                    return node.nd_lit().m_to_s();
                    
                case NODE_DSTR:
                case NODE_DXSTR:
                case NODE_DREGX:
                case NODE_DREGX_ONCE:
                    list = node.nd_next();
                    
                    RubyString str = (RubyString)node.nd_lit();
//                    RubyString str2 = null;
                    RubyString str2 = (RubyString)node.nd_next().nd_head().nd_lit();
                    
                    while (list != null) {
                        if (list.nd_head() != null) {
                            switch (list.nd_head().nd_type()) {
                                case NODE_STR:
                                    str2 = (RubyString)list.nd_head().nd_lit();
                                    break;
                                    
                                case NODE_EVSTR:
                                    // result = ruby_errinfo;
                                    // ruby_errinfo = Qnil;
                                    ruby.setSourceLine(node.nd_line());
                                    ruby.setInEval(ruby.getInEval() + 1);
                                    list.nd_head(ruby.getRubyParser().compileString(ruby.getSourceFile(), list.nd_head().nd_lit(), ruby.getSourceLine()));
                                    ruby.getParserHelper().setEvalTree(null);
                                    ruby.setInEval(ruby.getInEval() - 1);
                                    // if (ruby_nerrs > 0) {
                                    //    compile_error("string expansion");
                                    // }
                                    // if (!NIL_P(result)) ruby_errinfo = result;
                                /* fall through */
                                default:
                                    str2 = (RubyString)eval(self, list.nd_head()).convertType(RubyString.class, "String", "to_s");
                                    break;
                            }
                            
                            str.m_concat(str2);
//                            str.infectObject(str2);
                        }
                        list = list.nd_next();
                    }
                    switch (node.nd_type()) {
                        case NODE_DREGX:
                            return RubyRegexp.m_newRegexp(ruby, str, node.nd_cflag().intValue());
                            
                        case NODE_DREGX_ONCE:	/* regex expand once */
                            RubyObject regex = RubyRegexp.m_newRegexp(ruby, str, node.nd_cflag().intValue());
                            node.nd_set_type(NODE_LIT);
                            node.nd_lit(regex);
                            return regex;
                            
                        case NODE_DXSTR:
                            return self.funcall(ruby.intern("`"), str);
                            
                        default:
                            return str;
                    }
//                    return null;
                    
                case NODE_XSTR:
                    return self.funcall(ruby.intern("`"), node.nd_lit());
                    
                case NODE_LIT:
                    return node.nd_lit();
                    
                case NODE_ATTRSET:
                    if (ruby.getRubyFrame().getArgs().size() != 1) {
                        throw new RubyArgumentException("wrong # of arguments(" + ruby.getRubyFrame().getArgs().size() + "for 1)");
                    }
                    return self.setInstanceVar(node.nd_vid(), (RubyObject)ruby.getRubyFrame().getArgs().get(0));
                    
                case NODE_DEFN:
                    if (node.nd_defn() != null) {
                        int noex;
                        if (ruby.getRubyClass() == null) {
                            throw new RubyTypeException("no class to add method");
                        }
                        
                        //if (ruby_class == getRuby().getObjectClass() && node.nd_mid() == init) {
                            // rom.rb_warn("redefining Object#initialize may cause infinite loop");
                        //}
                        //if (node.nd_mid() == __id__ || node.nd_mid() == __send__) {
                            // rom.rb_warn("redefining `%s' may cause serious problem", ((RubyId)node.nd_mid()).toName());
                        //}
                        // ruby_class.setFrozen(true);
                        
                        SearchMethodResult smr = ruby.getRubyClass().searchMethod((RubyId)node.nd_mid());
                        NODE body = smr.getBody();
                        RubyObject origin = smr.getOrigin();
                        
                        if (body != null){
                            // if (ruby_verbose.isTrue() && ruby_class == origin && body.nd_cnt() == 0) {
                            //     rom.rb_warning("discarding old %s", ((RubyId)node.nd_mid()).toName());
                            // }
                            // if (node.nd_noex() != 0) { /* toplevel */
                            /* should upgrade to rb_warn() if no super was called inside? */
                            //     rom.rb_warning("overriding global function `%s'", ((RubyId)node.nd_mid()).toName());
                            // }
                        }
                        if (isScope(SCOPE_PRIVATE) || node.nd_mid().equals(ruby.intern("initialize"))) {
                            noex = NOEX_PRIVATE;
                        } else if (isScope(SCOPE_PROTECTED)) {
                            noex = NOEX_PROTECTED;
                        } else if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {
                            noex =  node.nd_noex();
                        } else {
                            noex = NOEX_PUBLIC;
                        }
                        if (body != null && origin == ruby.getRubyClass() && (body.nd_noex() & NOEX_UNDEF) != 0) {
                            noex |= NOEX_UNDEF;
                        }
                        
                        NODE defn = node.nd_defn().copyNodeScope(ruby.getRubyCRef());
                        ruby.getRubyClass().addMethod(node.nd_mid(), defn, noex);
                        // rb_clear_cache_by_id(node.nd_mid());
                        if (actMethodScope == SCOPE_MODFUNC) {
                            ruby.getRubyClass().getSingletonClass().addMethod(node.nd_mid(), defn, NOEX_PUBLIC);
                            ruby.getRubyClass().funcall(ruby.intern("singleton_method_added"), node.nd_mid().toSymbol());
                        }
                        if (ruby.getRubyClass().isSingleton()) {
                            ruby.getRubyClass().getInstanceVar("__attached__").funcall(ruby.intern("singleton_method_added"), node.nd_mid().toSymbol());
                        } else {
                            ruby.getRubyClass().funcall(ruby.intern("method_added"), node.nd_mid().toSymbol());
                        }
                    }
                    return ruby.getNil();
                    
                case NODE_DEFS:
                    if (node.nd_defn() != null) {
                        recv = eval(self, node.nd_recv());
                        
                        if (ruby.getSecurityLevel() >= 4 && !recv.isTaint()) {
                            throw new RubySecurityException("Insecure; can't define singleton method");
                        }
                        /*if (FIXNUM_P(recv) || SYMBOL_P(recv)) {
                            rb_raise(rb_eTypeError, "can't define singleton method \"%s\" for %s",
                            rb_id2name(node.nd_mid()), rb_class2name(CLASS_OF(recv)));
                        }*/ // not needed in jruby  
                        
                        if (recv.isFrozen()) {
                            throw new RubyFrozenException("object");
                        }
                        rubyClass = recv.getSingletonClass();
                        
                        NODE body = (NODE)rubyClass.getMethods().get((RubyId)node.nd_mid());
                        if (body != null) {
                            if (ruby.getSecurityLevel() >= 4) {
                                throw new RubySecurityException("redefining method prohibited");
                            }
                            /*if (RTEST(ruby_verbose)) {
                                rb_warning("redefine %s", rb_id2name(node.nd_mid()));
                            }*/
                        }
                        NODE defn = node.nd_defn().copyNodeScope(ruby.getRubyCRef());
                        defn.nd_rval(ruby.getRubyCRef());
                        rubyClass.addMethod(node.nd_mid(), defn, NOEX_PUBLIC | (body != null ? body.nd_noex() & NOEX_UNDEF : 0));
                        // rb_clear_cache_by_id(node.nd_mid());
                        recv.funcall(ruby.intern("singleton_method_added"), node.nd_mid().toSymbol());
                    }
                    return ruby.getNil();
                    
                case NODE_UNDEF:
                    if (ruby.getRubyClass() == null) {
                        throw new RubyTypeException("no class to undef method");
                    }
                    ruby.getRubyClass().undef(node.nd_mid());
                    
                    return ruby.getNil();
                    
                case NODE_ALIAS:
                    if (ruby.getRubyClass() == null) {
                        throw new RubyTypeException("no class to make alias");
                    }
                    ruby.getRubyClass().aliasMethod(node.nd_new(), node.nd_old());
                    ruby.getRubyClass().funcall(ruby.intern("method_added"), node.nd_mid().toSymbol());
                    
                    return ruby.getNil();
                    
                case NODE_VALIAS:
                    RubyGlobalEntry.getGlobalEntry(node.nd_old()).alias(node.nd_new());
                    
                    return ruby.getNil();
                    
                case NODE_CLASS:
                    RubyModule superClass;
                    
                    if (ruby.getRubyClass() == null) {
                        throw new RubyTypeException("no outer class/module");
                    }
                    if (node.nd_super() != null) {
                        superClass = getSuperClass(self, node.nd_super());
                    } else {
                        superClass = null;
                    }
                    
                    rubyClass = null;
                    // if ((ruby_class == getRuby().getObjectClass()) && rb_autoload_defined(node.nd_cname())) {
                    //     rb_autoload_load(node.nd_cname());
                    // }
                    if (ruby.getRubyClass().isConstantDefined(node.nd_cname())) {
                        rubyClass = (RubyClass)ruby.getRubyClass().getConstant(node.nd_cname());
                    }
                    if (rubyClass != null) {
                        if (!rubyClass.isClass()) {
                            throw new RubyTypeException(node.nd_cname().toName() + " is not a class");
                        }
                        if (superClass != null) {
                            RubyModule tmp = rubyClass.getSuperClass();
                            if (tmp.isSingleton()) {
                                tmp = tmp.getSuperClass();
                            }
                            while (tmp.isIncluded()) {
                                tmp = tmp.getSuperClass();
                            }
                            if (tmp != superClass) {
                                superClass = tmp;
                                //goto override_class;
                                if (superClass == null) {
                                    superClass = ruby.getClasses().getObjectClass();
                                }
                                rubyClass = ruby.defineClassId(node.nd_cname(), (RubyClass)superClass);
                                ruby.getRubyClass().setConstant(node.nd_cname(), rubyClass);
                                rubyClass.setClassPath(ruby.getRubyClass(), node.nd_cname().toName());
                                // end goto
                            }
                        }
                        if (ruby.getSecurityLevel() >= 4) {
                            throw new RubySecurityException("extending class prohibited");
                        }
                        // rb_clear_cache();
                    } else {
                        //override_class:
                        if (superClass == null) {
                            superClass = ruby.getClasses().getObjectClass();
                        }
                        rubyClass = ruby.defineClassId(node.nd_cname(), (RubyClass)superClass);
                        ruby.getRubyClass().setConstant(node.nd_cname(), rubyClass);
                        rubyClass.setClassPath(ruby.getRubyClass(), node.nd_cname().toName());
                    }
                    if (ruby_wrapper != null) {
                        rubyClass.getSingletonClass().includeModule(ruby_wrapper);
                        rubyClass.includeModule(ruby_wrapper);
                    }
                    
                    return setupModule(rubyClass, node.nd_body());
                    
                case NODE_MODULE:
                    if (ruby.getRubyClass() == null) {
                        throw new RubyTypeException("no outer class/module");
                    }
                    
                    RubyModule module = null;
                    
                    if ((ruby.getRubyClass() == ruby.getClasses().getObjectClass()) && ruby.isAutoloadDefined(node.nd_cname())) {
                        // getRuby().rb_autoload_load(node.nd_cname());
                    }
                    if (ruby.getRubyClass().isConstantDefined(node.nd_cname())) {
                        module = (RubyModule)ruby.getRubyClass().getConstant(node.nd_cname());
                    }
                    if (module != null) {
                        if (!(module instanceof RubyModule)) {
                            throw new RubyTypeException(node.nd_cname().toName() + " is not a module");
                            
                        }
                        if (ruby.getSecurityLevel() >= 4) {
                            throw new RubySecurityException("extending module prohibited");
                        }
                    } else {
                        module = ruby.defineModuleId(node.nd_cname());
                        ruby.getRubyClass().setConstant(node.nd_cname(), module);
                        module.setClassPath(ruby.getRubyClass(), node.nd_cname().toName());
                    }
                    if (ruby_wrapper != null) {
                        module.getSingletonClass().includeModule(ruby_wrapper);
                        module.includeModule(ruby_wrapper);
                    }
                    
                    return setupModule(module, node.nd_body());
                    
                case NODE_SCLASS:
                    rubyClass = (RubyClass)eval(self, node.nd_recv());
                    if (rubyClass.isSpecialConst()) {
                        throw new RubyTypeException("no virtual class for " + rubyClass.getRubyClass().toName());
                    }
                    if (ruby.getSecurityLevel() >= 4 && !rubyClass.isTaint()) {
                        throw new RubySecurityException("Insecure: can't extend object");
                    }
                    if (rubyClass.getRubyClass().isSingleton()) {
                        // rb_clear_cache();
                    }
                    rubyClass = rubyClass.getSingletonClass();
                    
                    if (ruby_wrapper != null) {
                        rubyClass.getSingletonClass().includeModule(ruby_wrapper);
                        rubyClass.includeModule(ruby_wrapper);
                    }
                    
                    return setupModule(rubyClass, node.nd_body());
                    
                case NODE_DEFINED:
                    // String buf;
                    // String desc = is_defined(self, node.nd_head(), buf);
                    // 
                    // if (desc) {
                    //     result = rb_str_new2(desc);
                    // } else {
                    //     result = Qnil;
                    // }
                    
                case NODE_NEWLINE:
                    ruby.setSourceFile(node.nd_file);
                    ruby.setSourceLine(node.nd_nth());
                    // if (trace_func) {
                    //     call_trace_func("line", ruby_sourcefile, ruby_sourceline, self,
                    //     ruby_frame.last_func(),
                    //     ruby_frame.last_class());
                    // }
                    node = node.nd_next();
                    break;
                    
                default:
                    throw new RubyBugException("unknown node type " + node.nd_type());
            }
        }
    }
    
    private RubyStack classStack = new RubyStack(new LinkedList());
    
    public void pushClass() {
        classStack.push(ruby.getRubyClass());
    }

    private void popClass() {
        ruby.setRubyClass((RubyModule)classStack.pop());
    }

    private void PUSH_CREF(Object c) {
        ruby.setRubyCRef(new NODE(NODE.NODE_CREF, c, null, ruby.getRubyCRef()));
    }
    
    private void  POP_CREF() {
        ruby.setRubyCRef(ruby.getRubyCRef().nd_next());
    }

    
    /** ev_const_get
     *
     */
    protected RubyObject getConstant(NODE cref, RubyId id, RubyObject self) {
        NODE cbase = cref;
        
        // HACK +++
        if (ruby.getClasses().getClassMap().get(id) != null) {
            return (RubyObject)ruby.getClasses().getClassMap().get(id);
        }
        // HACK ---
        
        while (cbase != null && cbase.nd_next() != null) {
            RubyObject rubyClass = (RubyObject)cbase.nd_clss();
            if (rubyClass.isNil()) {
                return self.getRubyClass().getConstant(id);
            } else if (rubyClass.getInstanceVariables().get(id) != null) {
                return (RubyObject)rubyClass.getInstanceVariables().get(id);
            }
            cbase = cbase.nd_next();
        }
        return ((RubyModule)cref.nd_clss()).getConstant(id);
    }
    
    public RubyObject setupModule(RubyModule module, NODE n) {
        NODE node = n;
        
        // String file = ruby_sourcefile;
        // int line = ruby_sourceline;
        
        // TMP_PROTECT;

        RubyFrame frame = ruby.getRubyFrame();
        frame.setTmp(ruby.getRubyFrame());
        ruby.setRubyFrame(frame);

        pushClass();
        ruby.setRubyClass(module);
        ruby.getRubyScope().push();
        RubyVarmap.push(ruby);

        if (node.nd_tbl() != null) {
            List tmp = Collections.nCopies(node.nd_tbl()[0].intValue() + 1, ruby.getNil());
            ShiftableList vars = new ShiftableList(new ArrayList(tmp));
            vars.set(0, (VALUE)node);
            vars.shift(1);
            ruby.getRubyScope().setLocalTbl(node.nd_tbl());
        } else {
            ruby.getRubyScope().setLocalVars(null);
            ruby.getRubyScope().setLocalTbl(null);
        }

        PUSH_CREF(module);
        ruby.getRubyFrame().setCbase(ruby.getRubyCRef());
        // PUSH_TAG(PROT_NONE);
        
        RubyObject result = null;
        
        // if (( state = EXEC_TAG()) == 0 ) {
            // if (trace_func) {
            //     call_trace_func("class", file, line, ruby_class, 
            //                     ruby_frame->last_func, ruby_frame->last_class );
            // }
            result = eval(ruby.getRubyClass(), node.nd_next());
        // }
            
        // POP_TAG();
        POP_CREF();
        RubyVarmap.pop(ruby);
        ruby.getRubyScope().pop();
        popClass();

        ruby.setRubyFrame(frame.getTmp());
//        if (trace_func) {
//            call_trace_func("end", file, line, 0, ruby_frame->last_func, ruby_frame->last_class );
//        }
        // if (state != 0) {
        //     JUMP_TAG(state);
        // }

        return result;
    }

    
    // C constants and variables
    
    /** massign
     *
     */
    public RubyObject massign(RubyObject self, NODE node, RubyObject val, boolean check) {
        if (val == null) {
            val = RubyArray.m_newArray(ruby);
        } else if (!(val instanceof RubyArray)) {
            // if ( rb_respond_to( val, to_ary ) ) {
                // val.funcall(getRuby().intern("to_a"));
            // } else {
                val = RubyArray.m_newArray(ruby, val);
            // }
        }
        
        int len = (int)((RubyArray)val).length();
        NODE list = node.nd_head();
        
        int i = 0;
        for (; list != null && i < len; i++) {
            assign(self, list.nd_head(), ((RubyArray)val).entry(i), check);
            list = list.nd_next();
        }
        
        if (check && list != null) {
            // error
        }
        
        if (node.nd_args() != null) {
            if (node.nd_args() == NODE.MINUS_ONE) {
            } else if (list == null && i < len) {
                assign(self, node.nd_args(), ((RubyArray)val).subseq(len - i, i), check);
            } else {
                assign(self, node.nd_args(), RubyArray.m_newArray(ruby), check);
            }
        } else if (check && i < len) {
            // error
        }
        
        while (list != null) {
            i++;
            assign(self, list.nd_head(), ruby.getNil(), check);
            list = list.nd_next();
        }
        return val;
    }

    /** assign
     *
     */
    public void assign(RubyObject self, NODE lhs, RubyObject val, boolean check) {
        if (val == null) {
            val = ruby.getNil();
        }
        switch (lhs.nd_type()) {
            case NODE_GASGN:
                lhs.nd_entry().set(val);
                break;

            case NODE_IASGN:
                self.setInstanceVar(lhs.nd_vid(), val);
                break;

            case NODE_LASGN:
                // if (getRuby().ruby_scope.local_vars == null) {
                //    rb_bug( "unexpected local variable assignment" );
                // }
                ruby.getRubyScope().setLocalVars(lhs.nd_cnt(), val);
                break;

            case NODE_DASGN:
                RubyVarmap.assign(ruby, lhs.nd_vid(), val);
                break;

            case NODE_DASGN_CURR:
                RubyVarmap.assignCurrent(ruby, lhs.nd_vid(), val);
                break;

            case NODE_CDECL:
                ruby.getRubyClass().setConstant(lhs.nd_vid(), val);
                break;

            case NODE_CVDECL:
                if (!ruby_cbase.isSingleton()) {
                    ruby_cbase.declareClassVar(lhs.nd_vid(), val);
                    break;
                }
                self = ruby_cbase.getInstanceVar("__attached__");
                /* fall through */
            case NODE_CVASGN:
                self.getClassVarSingleton().setClassVar(lhs.nd_vid(), val);
                break;

            case NODE_MASGN:
                massign(self, lhs, val, check);
                break;

            case NODE_CALL:
                RubyObject recv = eval(self, lhs.nd_recv());
                if (lhs.nd_args() == null) {
                    /* attr set */
                    recv.getRubyClass().call(recv, lhs.nd_mid(), new RubyObject[] { val }, 0);
                } else {
                    RubyArray args = (RubyArray)eval(self, lhs.nd_args());
                    args.push(val);
                    recv.getRubyClass().call(recv, lhs.nd_mid(), (RubyObject[])args.getArray().toArray(new RubyObject[0]), 0);
                }
                break;

            default:
                // rb_bug( "bug in variable assignment" );
                break;
        }
    }

    
    /** SCOPE_TEST
     *
     */
    private boolean isScope(int scope) {
        return (actMethodScope & scope) != 0;
    }
    
    /** SETUP_ARGS
     *
     */
    private RubyObject[] setupArgs(RubyObject self, NODE anode) {
        NODE n = anode;
        if (n == null) {
            return new RubyObject[0];
        } else if (n.nd_type() == NODE_ARRAY) {
            int len = n.nd_alen();
            if (len > 0) {
                // String file = ruby_sourcefile;
                // int line = ruby_sourceline;
                n = anode;
                RubyObject[] args = new RubyObject[len];
                for (int i = 0; i < len; i++) {
                    args[i] = eval(self, n.nd_head());
                    n = n.nd_next();
                }
                // ruby_sourcefile = file;
                // ruby_sourceline = line;
                return args;
            }  else {
                return new RubyObject[0];
            }
        } else {
            RubyObject args = eval(self, n);
            // String file = ruby_sourcefile;
            // int line = ruby_sourceline;
            if (!(args instanceof RubyArray)) {
                args = RubyArray.m_newArray(ruby, args);
            }
                
            List argsList = ((RubyArray)args).getArray();
            // ruby_sourcefile = file;
            // ruby_sourceline = line;
            return (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]);
        }
    }
    
    public boolean isBlockGiven() {
        return ruby.getRubyFrame().getIter() != Iter.ITER_NOT;
    }

    public boolean isFBlockGiven() {
        return (ruby.getRubyFrame().getPrev() != null) && (ruby.getRubyFrame().getPrev().getIter() != Iter.ITER_NOT);
    }

    public RubyObject yield0(RubyObject value, RubyObject self, RubyModule klass, boolean acheck) {
        RubyObject result = ruby.getNil();
        
        if (!(isBlockGiven() || isFBlockGiven()) || (rubyBlock == null)) {
            throw new RuntimeException("yield called out of block");
        }
        
        RubyVarmap.push(ruby);
        pushClass();
        RubyBlock block = rubyBlock.getTmp();
        
        RubyFrame frame = block.frame;
        frame.setPrev(ruby.getRubyFrame());
        ruby.setRubyFrame(frame);
        
        NODE oldCRef = ruby.getRubyCRef();
        ruby.setRubyCRef((NODE)ruby.getRubyFrame().getCbase());
        
        RubyScope oldScope = ruby.getRubyScope();
        ruby.setRubyScope(block.scope);
        rubyBlock.pop();
        
        if ((block.flags & RubyBlock.BLOCK_D_SCOPE) != 0) {
            ruby.setDynamicVars(new RubyVarmap(null, null, block.dynamicVars));
        } else {
            ruby.setDynamicVars(block.dynamicVars);
        }
        
        ruby.setRubyClass((klass != null) ? klass : block.klass);
        if (klass == null) {
            self = (RubyObject)block.self;
        }
        
        NODE node = block.body;
        
        if (block.var != null) {
            // try {
                if (block.var == NODE.ONE) {
                    if (acheck && value != null && 
                        value instanceof RubyArray && ((RubyArray)value).length() != 0) {
                        
                        throw new RubyArgumentException("wrong # of arguments ("+ ((RubyArray)value).length() + " for 0)");
                    }
                } else {
                    if (block.var.nd_type() == NODE_MASGN) {
                        massign(self, block.var, value, acheck);
                    } else {
                        if (acheck && value != null && value instanceof RubyArray && 
                            ((RubyArray)value).length() == 1) {
                            
                            value = ((RubyArray)value).entry(0);
                        }
                        assign(self, block.var, value, acheck);
                    }
                }
            // } catch () {
            //    goto pop_state;
            // }
             
        } else {
            if (acheck && value != null && value instanceof RubyArray && 
                ((RubyArray)value).length() == 1) {
                            
                value = ((RubyArray)value).entry(0);
            }
        }
        
        rubyIter.push(block.iter);
        while (true) {
            try {
                if (node == null) {
                    result = ruby.getNil();
                } else if (node.nd_type() == NODE_CFUNC || node.nd_type() == NODE_IFUNC) {
                    if (value == null) {
                        value = RubyArray.m_newArray(ruby, 0);
                    }
                    result = ((RubyCallbackMethod)node.nd_cfnc()).execute(value, new RubyObject[] {(RubyObject)node.nd_tval(), self}, ruby);
                } else {
                    result = eval(self, node);
                }
                break;
            } catch (RedoException rExcptn) {
            } catch (NextException nExcptn) {
                result = ruby.getNil();
                break;
            } catch (BreakException bExcptn) {
                break;
            } catch (ReturnException rExcptn) {
                break;
            }
        }
        
        // pop_state:
        
        rubyIter.pop();
        popClass();
        RubyVarmap.pop(ruby);
        
        rubyBlock.setTmp(block);
        ruby.setRubyFrame(ruby.getRubyFrame().getPrev());
        
        ruby.setRubyCRef(oldCRef);
        
        // if (ruby_scope->flag & SCOPE_DONT_RECYCLE)
        //    scope_dup(old_scope);
        ruby.setRubyScope(oldScope);

        /*
         * if (state) {
         *    if (!block->tag) {
         *       switch (state & TAG_MASK) {
         *          case TAG_BREAK:
         *          case TAG_RETURN:
         *             jump_tag_but_local_jump(state & TAG_MASK);
         *             break;
         *       }
         *    }
         *    JUMP_TAG(state);
         * }
         */
        
        return result;
    }
    
    private RubyClass getSuperClass(RubyObject self, NODE node) {
        RubyObject obj;
        int state = 1; // unreachable
        
        // PUSH_TAG(PROT_NONE);
        // if ((state = EXEC_TAG()) == 0 ) {
            obj = eval(self, node);
        // }
        // POP_TAG();
        
        /*if (state != 0) {
            switch (node.nd_type()) {
                case NODE_COLON2:
                    throw new RubyTypeException("undefined superclass '" + ((RubyId)node.nd_mid()).toName() + "'");
                case NODE_CONST:
                    throw new RubyTypeException("undefined superclass '" + ((RubyId)node.nd_vid()).toName() + "'");
                default:
                    throw new RubyTypeException("undefined superclass");
            }
        //     JUMP_TAG(state);
        }*/
        if (!(obj instanceof RubyClass)) {
            throw new RuntimeException();
            // goto superclass_error;
        }
        if (((RubyClass)obj).isSingleton()) {
            throw new RubyTypeException("can't make subclass of virtual class");
        }
        return (RubyClass)obj;
    }

    private RubyBlock beginCallargs() {
        RubyBlock tmpBlock = rubyBlock.getTmp();
        if (rubyIter.getIter() == Iter.ITER_PRE) {
            rubyBlock.pop();
        }
        rubyIter.push(Iter.ITER_NOT);
        return tmpBlock;
    }
    
    private void endCallArgs(RubyBlock tmpBlock) {
        rubyBlock.setTmp(tmpBlock);
        rubyIter.pop();
    }
    
    /** Getter for property rubyIter.
     * @return Value of property rubyIter.
     */
    public Iter getRubyIter() {
        return rubyIter;
    }
    
    /** Setter for property rubyIter.
     * @param rubyIter New value of property rubyIter.
     */
    public void setRubyIter(Iter rubyIter) {
        this.rubyIter = rubyIter;
    }
    
    /** Getter for property rubyBlock.
     * @return Value of property rubyBlock.
     */
    public RubyBlock getRubyBlock() {
        return rubyBlock;
    }
    
    /** Setter for property rubyBlock.
     * @param rubyBlock New value of property rubyBlock.
     */
    public void setRubyBlock(RubyBlock rubyBlock) {
        this.rubyBlock = rubyBlock;
    }
    
    /** Getter for property actMethodScope.
     * @return Value of property actMethodScope.
     */
    public int getActMethodScope() {
        return actMethodScope;
    }
    
    /** Setter for property actMethodScope.
     * @param actMethodScope New value of property actMethodScope.
     */
    public void setActMethodScope(int actMethodScope) {
        this.actMethodScope = actMethodScope;
    }
    
}
