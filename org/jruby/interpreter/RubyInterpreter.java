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
    
    private Frame rubyFrame;
    private Frame topFrame;
    
    // public RubyVarmap ruby_dyna_vars = new RubyVarmap();
    
    private RubyVarmap dynamicVars = null;
    private Iter rubyIter = new Iter(); // HACK 
    
    // C
    
    private RubyModule ruby_class;
    private RubyModule ruby_cbase;
    private RubyModule ruby_wrapper;
    
    private int scope_vmode;
    
    public RubyInterpreter(Ruby ruby) {
        this.ruby = ruby;
        
        ruby_class = ruby.getObjectClass();
        ruby_cbase = null;
    }
    
    public Frame getRubyFrame() {
        if (rubyFrame == null) {
            rubyFrame = new Frame(ruby);
        }
        return rubyFrame;
    }
    
    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public org.jruby.Ruby getRuby() {
        return ruby;
    }
    
    /** Setter for property ruby.
     * @param ruby New value of property ruby.
     */
    public void setRuby(org.jruby.Ruby ruby) {
        this.ruby = ruby;
    }
    
    private boolean initialized = false;
    
    /** ruby_init
     *
     */
    public void init() {
        int state;

        if (initialized) {
            return;
        }
        initialized = true;

        topFrame = getRubyFrame();
        rubyIter = new Iter();

        // rb_origenviron = environ;

        // Init_stack(0);
        // Init_heap();
            
        getRuby().rubyScope.push();// PUSH_SCOPE();
        getRuby().rubyScope.setLocalVars(null);
        getRuby().rubyScope.setLocalVars(null);
        // top_scope = getRuby().ruby_scope;
            
        /* default visibility is private at toplevel */
        // SET_SCOPE(SCOPE_PRIVATE);

        // PUSH_TAG( PROT_NONE );
        // if ((state = EXEC_TAG()) == 0) {
            // rb_call_inits();
            ruby_class = getRuby().getObjectClass();
            // ruby_frame.self = ruby_top_self;
            top_cref = new NODE(NODE_CREF, getRuby().getObjectClass(), null, null);
            ruby_cref = top_cref;
            rubyFrame.setCbase((VALUE)ruby_cref);
            // rb_define_global_const( "TOPLEVEL_BINDING", rb_f_binding( ruby_top_self ) );
            // ruby_prog_init();
        // }
        // POP_TAG();
        // if (state != 0) {
            // error_print();
        // }
        getRuby().rubyScope.pop();// POP_SCOPE();
        // ruby_scope = top_scope;
    }

    
    /** rb_eval
     *
     */    
    public RubyObject eval(RubyObject self, NODE n) {
        NODE node = n;
        
        RubyBoolean cond = null;
        RubyObject[] args = null;
        RubyObject result = null;
        // int state;
        
        // RubyOriginalMethods rom = getRuby().getOriginalMethods();
        
        while (true) {
            
            if (node == null) {
                return getRuby().getNil();
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
                    return getRuby().getNil();
                    
                /* begin .. end without clauses */
                case NODE_BEGIN:
                    node = node.nd_body();
                    break;
                    
                /* nodes for speed-up(default match) */
                case NODE_MATCH:
                    //return rom.rb_reg_match2(node.nd_head().nd_lit());
                    return getRuby().getNil();
                    
                /* nodes for speed-up(literal match) */
                case NODE_MATCH2:
                    //return rom.rb_reg_match(eval(node.nd_recv()), eval(node.nd_value()));
                    return getRuby().getNil();
                    
                /* nodes for speed-up(literal match) */
                case NODE_MATCH3:
                    //VALUE r = eval(node.nd_recv());
                    //VALUE l = eval(node.nd_value());
                    //if (r instanceof RubyString) {
                    //    return rom.rb_reg_match(l, r);
                    //} else {
                    //    return rom.rb_funcall(r, match, 1, l);
                    //}
                    return getRuby().getNil();
                    
                /* node for speed-up(top-level loop for -n/-p) */
                case NODE_OPT_N:
/*                PUSH_TAG(PROT_NONE);
                switch (state = EXEC_TAG()) {
                    case 0:
                        opt_n_next:
                            while (!NIL_P(rb_gets())) {
                                opt_n_redo:
                            rb_eval(self, node->nd_body);
                          }
                    break;
 
                    case TAG_REDO:
                        state = 0;
            goto opt_n_redo;
          case TAG_NEXT:
            state = 0;
            goto opt_n_next;
          case TAG_BREAK:
            state = 0;
          default:
            break;
        }
        POP_TAG();
        if (state) JUMP_TAG(state);
        RETURN(Qnil);*/
                    
                    return getRuby().getNil();
                    
                case NODE_SELF:
                    return self;
                    
                case NODE_NIL:
                    return getRuby().getNil();
                    
                case NODE_TRUE:
                    return getRuby().getTrue();
                    
                case NODE_FALSE:
                    return getRuby().getFalse();
                    
                case NODE_IF:
                    //                ruby_sourceline = node.nd_line();
                    cond = (RubyBoolean)eval(self, node.nd_cond());
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
                                    obj = RubyArray.m_newArray(getRuby(), obj);
                                }
                                
                                for (int i = 0; i < ((RubyArray)obj).length(); i++) {
                                    if (((RubyBoolean)((RubyArray)obj).entry(i)).isTrue()) {
                                        node = node.nd_body();
                                        break;
                                    }
                                }
                                tag = tag.nd_next();
                                continue;
                            }
                            if (((RubyBoolean)eval(self, tag.nd_head())).isTrue()) {
                                node = node.nd_body();
                                break;
                            }
                            tag = tag.nd_next();
                        }
                        node = node.nd_next();
                    }
                    return getRuby().getNil();
                    
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
                        }
                    ruby_sourcefile = tag->nd_file;
                    ruby_sourceline = nd_line(tag);*/
                            
                            if (tag.nd_head().nd_type() == NODE_WHEN) {
                                RubyObject obj2 = eval(self, tag.nd_head().nd_head());
                                
                                if (!(obj2 instanceof RubyArray)) {
                                    obj2 = RubyArray.m_newArray(getRuby(), obj2);
                                }
                                for (int i = 0; i < ((RubyArray)obj).length(); i++) {
                                    RubyBoolean eqq = (RubyBoolean)((RubyArray)obj2).entry(i).funcall(getRuby().intern("==="), obj);
                                    if (eqq.isTrue()) {
                                        node = node.nd_body();
                                        break;
                                    }
                                }
                                tag = tag.nd_next();
                                continue;
                            }
                            if (((RubyBoolean)eval(self, tag.nd_head()).funcall(getRuby().intern("==="), obj)).isTrue()) {
                                node = node.nd_body();
                                break;
                            }
                            tag = tag.nd_next();
                        }
                        node = node.nd_next();
                    }
                    return getRuby().getNil();
                    
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
                                return getRuby().getNil();
                            }
                        }
                    }
                    
                    return getRuby().getNil();
                    
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
                                return getRuby().getNil();
                            }
                        }
                    }
                    
                    return getRuby().getNil();
                    
                case NODE_BLOCK_PASS:
                    //return block_pass(node);
                    return null;
                    
                case NODE_ITER:
                case NODE_FOR:
                    while (true) {
                        BLOCK _block = PUSH_BLOCK(node.nd_var(), node.nd_body(), self);
                        
                        try {
                            rubyIter.push(Iter.ITER_PRE);
                            if (node.nd_type() == NODE_ITER) {
                                result = eval(self, node.nd_iter());
                            } else {
                                // String file = 
                                // int line = 
                                
                                _block.flags &= ~BLOCK_D_SCOPE;
                                
                                BLOCK tmp_block = BEGIN_CALLARGS();
                                RubyObject recv = eval(self, node.nd_iter());
                                END_CALLARGS(tmp_block);
                                
                                // = file;
                                // = line;
                                result = recv.getRubyClass().call(recv, ruby.intern("each"), null, 0);
                            }
                            rubyIter.pop();
                            
                            break;
                        } catch (RetryException rExcptn) {
                            POP_BLOCK(_block);
                        } catch (ReturnException rExcptn) {
                            POP_BLOCK(_block);
                            result = rExcptn.getReturnValue();
                            
                            break;
                        } catch (BreakException bExcptn) {
                            POP_BLOCK(_block);
                            result = ruby.getNil();
                            break;
                        }
                    }
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
                        result = obj = RubyArray.m_newArray(getRuby(), result);
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
                    cond = (RubyBoolean)eval(self, node.nd_1st());
                    if (cond.isFalse()) {
                        return cond;
                    }
                    node = node.nd_2nd();
                    break;
                    
                case NODE_OR:
                    cond = (RubyBoolean)eval(self, node.nd_1st());
                    if (cond.isTrue()) {
                        return cond;
                    }
                    node = node.nd_2nd();
                    break;
                    
                case NODE_NOT:
                    return RubyBoolean.m_newBoolean(getRuby(), eval(self, node.nd_1st()).isFalse());
                    
                case NODE_DOT2:
                case NODE_DOT3:
                    result = RubyRange.m_newRange(getRuby(), eval(self, node.nd_beg()),
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
                    //RubyBoolean result;
/*                if (ruby_scope->local_vars == 0) {
                    rb_bug("unexpected local variable");
                }
                if (!RTEST(ruby_scope->local_vars[node->nd_cnt])) {
                    if (RTEST(rb_eval(self, node->nd_beg))) {
                        ruby_scope->local_vars[node->nd_cnt] =
                        RTEST(rb_eval(self, node->nd_end))?Qfalse:Qtrue;
                        result = Qtrue;
                    } else {
                        result = Qfalse;
                    }
                } else {
                    if (RTEST(rb_eval(self, node->nd_end))) {
                        ruby_scope->local_vars[node->nd_cnt] = Qfalse;
                    }
                    result = Qtrue;
                }*/
                    return result;
                    
                case NODE_FLIP3:		/* like SED */
                    //RubyBoolean result;
/*                if (ruby_scope->local_vars == 0) {
                    rb_bug("unexpected local variable");
                }
                if (!RTEST(ruby_scope->local_vars[node->nd_cnt])) {
                    result = RTEST(rb_eval(self, node->nd_beg)) ? Qtrue : Qfalse;
                    ruby_scope->local_vars[node->nd_cnt] = result;
                } else {
                    if (RTEST(rb_eval(self, node->nd_end))) {
                        ruby_scope->local_vars[node->nd_cnt] = Qfalse;
                    }
                    result = Qtrue;
                }*/
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
                    
                    BLOCK tmpBlock = BEGIN_CALLARGS();
                    
                    RubyObject recv = eval(self, node.nd_recv());
                    args = setupArgs(self, node.nd_args());
                    
                    END_CALLARGS(tmpBlock);
                    
                    return recv.getRubyClass().call(recv, (RubyId)node.nd_mid(), args, 0);
                    
                case NODE_FCALL:
                    //                TMP_PROTECT;
                    
                    tmpBlock = BEGIN_CALLARGS();
                    
                    args = setupArgs(self, node.nd_args());
                    
                    END_CALLARGS(tmpBlock);
                    
                    return self.getRubyClass().call(self, (RubyId)node.nd_mid(), args, 1);
                    
                case NODE_VCALL:
                    return self.getRubyClass().call(self, (RubyId)node.nd_mid(), null, 2);
                    
                case NODE_SUPER:
                case NODE_ZSUPER:
                    //                TMP_PROTECT;
                    
                    if (rubyFrame.getLastClass() == null) {
                        throw new RubyNameException("superclass method '" + rubyFrame.getLastFunc().toName() + "' disabled");
                    }
                    if (node.nd_type() == NODE_ZSUPER) {
                        List argsList = rubyFrame.getArgs();
                        args = (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]);
                    } else {
                        tmpBlock = BEGIN_CALLARGS();
                        args = setupArgs(self, node.nd_args());
                        END_CALLARGS(tmpBlock);
                    }
 
                    rubyIter.push(rubyIter.getIter() != Iter.ITER_NOT ? Iter.ITER_PRE : Iter.ITER_NOT);
                    result = rubyFrame.getLastClass().getSuperClass().call(rubyFrame.getSelf(), rubyFrame.getLastFunc(), args, 3);
                    rubyIter.pop();
                    return result;
                    
                case NODE_SCOPE:
                    /*NODE saved_cref = null;
 
                    FRAME frame = ruby_frame;
                    frame.tmp = ruby_frame;
                    ruby_frame = frame;
 
                    PUSH_SCOPE();
                    PUSH_TAG(PROT_NONE);
                    if (node->nd_rval) {
                        saved_cref = ruby_cref;
                        ruby_cref = (NODE*)node->nd_rval;
                        ruby_frame->cbase = node->nd_rval;
                    }*/
                    
                    if (node.nd_tbl() != null) {
                        List tmp = Collections.nCopies(node.nd_tbl()[0].intValue() + 1, ruby.getNil());
                        ShiftableList vars = new ShiftableList(new ArrayList(tmp));
                        vars.set(0, (VALUE)node);
                        vars.shift(1);
                        getRuby().rubyScope.setLocalVars(vars);
                        getRuby().rubyScope.setLocalTbl(node.nd_tbl());
                    } else {
                        getRuby().rubyScope.setLocalVars(null);
                        getRuby().rubyScope.setLocalTbl(null);
                    }
/*            if ((state = EXEC_TAG()) == 0) {
                result = rb_eval(self, node->nd_next);
            }
            POP_TAG();
            POP_SCOPE();
            ruby_frame = frame.tmp;
            if (saved_cref)
                ruby_cref = saved_cref;
            if (state) JUMP_TAG(state);
        }
        break;
 */
                case NODE_OP_ASGN1:
                    //                TMP_PROTECT;
                    
                    recv = eval(self, node.nd_recv());
                    NODE rval = node.nd_args().nd_head();
                    
                    args = setupArgs(self, node.nd_args().nd_next());
                    
                    ArrayList argsList = new ArrayList(Arrays.asList(args));
                    argsList.remove(args.length - 1);                    
                    RubyBoolean val = (RubyBoolean)recv.funcall(getRuby().intern("[]"), (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]));
                    
                    switch (node.nd_mid().intValue()) {
                        case 0: /* OR */
                            if (val.isTrue()) {
                                return val;
                            }
                            val = (RubyBoolean)eval(self, rval);
                            break;
                        case 1: /* AND */
                            if (val.isFalse()) {
                                return val;
                            }
                            val = (RubyBoolean)eval(self, rval);
                            break;
                        default:
                            val = (RubyBoolean)val.funcall((RubyId)node.nd_mid(), eval(self, rval));
                    }
                    args[args.length - 1] = val;
                    return recv.funcall(getRuby().intern("[]="), args);
                    
                case NODE_OP_ASGN2:
                    ID id = node.nd_next().nd_vid();
                    
                    recv = eval(self, node.nd_recv());
                    val = (RubyBoolean)recv.funcall((RubyId)id, (RubyObject[])null);
                    
                    switch (node.nd_next().nd_mid().intValue()) {
                        case 0: /* OR */
                            if (val.isTrue()) {
                                return val;
                            }
                            val = (RubyBoolean)eval(self, node.nd_value());
                            break;
                        case 1: /* AND */
                            if (val.isFalse()) {
                                return val;
                            }
                            val = (RubyBoolean)eval(self, node.nd_value());
                            break;
                        default:
                            val = (RubyBoolean)val.funcall((RubyId)node.nd_mid(), eval(self, node.nd_value()));
                    }
                    
                    // rom.rb_funcall2(recv, node.nd_next().nd_aid(), 1, &val);
                    return val;
                    
                case NODE_OP_ASGN_AND:
                    cond = (RubyBoolean)eval(self, node.nd_head());
                    
                    if (cond.isFalse()) {
                        return cond;
                    }
                    node = node.nd_value();
                    
                    break;
                    
                case NODE_OP_ASGN_OR:
                    cond = (RubyBoolean)eval(self, node.nd_head());
                    
                    if ((node.nd_aid() != null && !self.isInstanceVarDefined((RubyId)node.nd_aid())) || cond.isFalse()) {
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
                    getRuby().rubyScope.setLocalVars(node.nd_cnt(), result);
                    return result;
                    
                case NODE_DASGN:
                    result = eval(self, node.nd_value());
                    RubyVarmap.assign(ruby, (RubyId)node.nd_vid(), result);
                    return result;
                    
                case NODE_DASGN_CURR:
                    result = eval(self, node.nd_value());
                    RubyVarmap.assignCurrent(ruby, (RubyId)node.nd_vid(), result);
                    return result;
                    
                case NODE_GASGN:
                    result = eval(self, node.nd_value());
                    ((RubyGlobalEntry)node.nd_entry()).set(result);
                    return result;
                    
                case NODE_IASGN:
                    result = eval(self, node.nd_value());
                    self.setInstanceVar((RubyId)node.nd_vid(), result);
                    return result;
                    
                case NODE_CDECL:
                    if (ruby_class == null) {
                        throw new RubyTypeException("no class/module to define constant");
                    }
                    result = eval(self, node.nd_value());
                    ruby_class.setConstant((RubyId)node.nd_vid(), result);
                    return result;
                    
                case NODE_CVDECL:
                    if (ruby_cbase == null) {
                        throw new RubyTypeException("no class/module to define class variable");
                    }
                    result = eval(self, node.nd_value());
                    if (ruby_cbase.isSingleton()) {
                        ruby_cbase.getInstanceVar("__attached__").getClassVarSingleton().declareClassVar((RubyId)node.nd_vid(), result);
                        return result;
                    }
                    ruby_cbase.declareClassVar((RubyId)node.nd_vid(), result);
                    return result;
                    
                case NODE_CVASGN:
                    result = eval(self, node.nd_value());
                    self.getClassVarSingleton().setClassVar((RubyId)node.nd_vid(), result);
                    return result;
                    
                case NODE_LVAR:
                    //if (getRuby().ruby_scope.local_vars == null) {
                    //     rb_bug("unexpected local variable");
                    // }
                    return (RubyObject)getRuby().rubyScope.getLocalVars(node.nd_cnt());
                    
                case NODE_DVAR:
                    return getDynamicVars().getRef((RubyId)node.nd_vid());
                    
                case NODE_GVAR:
                    return ((RubyGlobalEntry)node.nd_entry()).get();
                    
                case NODE_IVAR:
                    return self.getInstanceVar((RubyId)node.nd_vid());
                    
                case NODE_CONST:
                    return getConstant((NODE)rubyFrame.getCbase(), (RubyId)node.nd_vid(), self);
                    
                case NODE_CVAR:     /* normal method */
                    if (ruby_cbase == null) {
                        return self.getRubyClass().getClassVar((RubyId)node.nd_vid());
                    }
                    if (!ruby_cbase.isSingleton()) {
                        return ruby_cbase.getClassVar((RubyId)node.nd_vid());
                    }
                    
                    return ruby_cbase.getInstanceVar("__attached__").getClassVarSingleton().getClassVar((RubyId)node.nd_vid());
                    
                case NODE_CVAR2:		/* singleton method */
                    return self.getClassVarSingleton().getClassVar((RubyId)node.nd_vid());
                    
                case NODE_BLOCK_ARG:
                    if (ruby.rubyScope.getLocalVars() == null) {
                        throw new RuntimeException("BUG: unexpected block argument");
                    }
                    if (isBlockGiven()) {
                        result = getRuby().getNil(); // Create Proc object
                        ruby.rubyScope.setLocalVars(node.nd_cnt(), result);
                        return result;
                    } else {
                        return getRuby().getNil();
                    }
                    
                case NODE_COLON2:
                    RubyModule rubyClass = (RubyModule)eval(self, node.nd_head());
/*                    switch (TYPE(klass)) {
                        case T_CLASS:
                        case T_MODULE:
                            break;
                        default:
                            return rom.rb_funcall(klass, node.nd_mid(), 0, 0);
                    }*/
                    return rubyClass.getConstant((RubyId)node.nd_mid());
                    
                case NODE_COLON3:
                    return getRuby().getObjectClass().getConstant((RubyId)node.nd_mid());
                    
                case NODE_NTH_REF:
                    // return rom.rb_reg_nth_match(node.nd_nth(), MATCH_DATA);
                    return null;
                    
                case NODE_BACK_REF:
                    /*switch ((char)node.nd_nth()) {
                        case '&':
                            return rom.rb_reg_last_match(MATCH_DATA);
                            
                        case '`':
                            return rom.rb_reg_match_pre(MATCH_DATA);
                            
                        case '\'':
                            return rom.rb_reg_match_post(MATCH_DATA);
                            
                        case '+':
                            return rom.rb_reg_match_last(MATCH_DATA);
                            
                        default:
                            rom.rb_bug("unexpected back-ref");
                    }*/
                    
                case NODE_HASH:
/*                    RubyHash hash = RubyHash.m_newHash();
                    RubyObject key, val;
                    
                    NODE list = node.nd_head();
                    while (list != null) {
                        key = eval(self, list.nd_head());
                        list = list.nd_next();
                        if (list == null) {
                            rom.rb_bug("odd number list for Hash");
                        }
                        val = eval(self, list.nd_head());
                        list = list.nd_next();
                        hash.aset(key, val);
                    }
                    return hash;*/
                    
                case NODE_ZARRAY:		/* zero length list */
                    return RubyArray.m_newArray(getRuby());
                    
                case NODE_ARRAY:
                    ArrayList ary = new ArrayList(node.nd_alen());
                    for (; node != null ; node = node.nd_next()) {
                        ary.add(eval(self, node.nd_head()));
                    }
                    return RubyArray.m_newArray(getRuby(), ary);
                    
                case NODE_STR:
                    return ((RubyObject)node.nd_lit()).m_to_s();
                    
                case NODE_DSTR:
                case NODE_DXSTR:
                case NODE_DREGX:
                case NODE_DREGX_ONCE:
/*                    NODE list = node.nd_next();
                    
                    RubyString str = RubyString.m_newString(getRuby(), (RubyObject)node.nd_lit());
                    RubyString str2;
                    
                    while (list != null) {
                        if (list.nd_head() != null) {
                            switch (list.nd_head().nd_type()) {
                                case NODE_STR:
                                    str2 = (RubyString)list.nd_head().nd_lit();
                                    break;
                                    
                                case NODE_EVSTR:
                                    result = ruby_errinfo;
                                    ruby_errinfo = Qnil;
                                    ruby_sourceline = nd_line(node);
                                    ruby_in_eval++;
                                    list.nd_head(compile(list.nd_head().nd_lit(), ruby_sourcefile,ruby_sourceline));
                                    ruby_eval_tree = 0;
                                    ruby_in_eval--;
                                    if (ruby_nerrs > 0) {
                                        compile_error("string expansion");
                                    }
                                    if (!NIL_P(result)) ruby_errinfo = result;
                        /* fall through */
/*                                default:
                                    str2 = (RubyString)rom.rb_obj_as_string(eval(list.nd_head()));
                                    break;
                            }
                            
                            str.append(str2);
                            str.infectObject(str2);
                        }
                        list = list.nd_next();
                    }
                    switch (node.nd_type()) {
                        case NODE_DREGX:
                            return rom.rb_reg_new(str.getString(), str.getString().length(), node.nd_cflag());
                            
                        case NODE_DREGX_ONCE:	/* regexp expand once */
/*                            VALUE result = rom.rb_reg_new(str.getString(), str.getString().length(), node.nd_cflag());
                            node.nd_set_type(NODE_LIT);
                            node.nd_lit(result);
                            return result;
                            
                        case NODE_DXSTR:
                            return rom.rb_funcall(this, '`', 1, str);
                            
                        default:
                            return str;
                    }*/
                    return null;
                    
                case NODE_XSTR:
                    return self.funcall(getRuby().intern("`"), (RubyObject)node.nd_lit());
                    
                case NODE_LIT:
                    return (RubyObject)node.nd_lit();
                    
                case NODE_ATTRSET:
                    if (rubyFrame.getArgs().size() != 1) {
                        throw new RubyArgumentException("wrong # of arguments(" + rubyFrame.getArgs().size() + "for 1)");
                    }
                    return self.setInstanceVar((RubyId)node.nd_vid(), (RubyObject)rubyFrame.getArgs().get(0));
                    
                case NODE_DEFN:
                    if (node.nd_defn() != null) {
                        int noex;
                        if (ruby_class == null) {
                            throw new RubyTypeException("no class to add method");
                        }
                        
                        //if (ruby_class == getRuby().getObjectClass() && node.nd_mid() == init) {
                            // rom.rb_warn("redefining Object#initialize may cause infinite loop");
                        //}
                        //if (node.nd_mid() == __id__ || node.nd_mid() == __send__) {
                            // rom.rb_warn("redefining `%s' may cause serious problem", ((RubyId)node.nd_mid()).toName());
                        //}
                        // ruby_class.setFrozen(true);
                        
                        NODE body = ruby_class.searchMethod((RubyId)node.nd_mid());
                        RubyObject origin = ruby_class.getMethodOrigin((RubyId)node.nd_mid());
                        
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
                        } else if (ruby_class == getRuby().getObjectClass()) {
                            noex =  node.nd_noex();
                        } else {
                            noex = NOEX_PUBLIC;
                        }
                        if (body != null && origin == ruby_class && (body.nd_noex() & NOEX_UNDEF) != 0) {
                            noex |= NOEX_UNDEF;
                        }
                        
                        NODE defn = node.nd_defn().copyNodeScope(ruby_cref);
                        ruby_class.addMethod((RubyId)node.nd_mid(), defn, noex);
                        // rb_clear_cache_by_id(node.nd_mid());
                        if (scope_vmode == SCOPE_MODFUNC) {
                            ruby_class.getSingletonClass().addMethod((RubyId)node.nd_mid(), defn, NOEX_PUBLIC);
                            ruby_class.funcall(getRuby().intern("singleton_method_added"), ((RubyId)node.nd_mid()).toSymbol());
                        }
                        if (ruby_class.isSingleton()) {
                            ruby_class.getInstanceVar("__attached__").funcall(getRuby().intern("singleton_method_added"), ((RubyId)node.nd_mid()).toSymbol());
                        } else {
                            // ruby_class.funcall(getRuby().intern("method_added"), ((RubyId)node.nd_mid()).toSymbol());
                        }
                    }
                    return getRuby().getNil();
                    
                case NODE_DEFS:
                    if (node.nd_defn() != null) {
                        recv = eval(self, node.nd_recv());
                        
                        if (getRuby().getSecurityLevel() >= 4 && !recv.isTaint()) {
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
                            if (getRuby().getSecurityLevel() >= 4) {
                                throw new RubySecurityException("redefining method prohibited");
                            }
                            /*if (RTEST(ruby_verbose)) {
                                rb_warning("redefine %s", rb_id2name(node.nd_mid()));
                            }*/
                        }
                        NODE defn = node.nd_defn().copyNodeScope(ruby_cref);
                        defn.nd_rval(ruby_cref);
                        rubyClass.addMethod((RubyId)node.nd_mid(), defn, NOEX_PUBLIC | (body != null ? body.nd_noex() & NOEX_UNDEF : 0));
                        // rb_clear_cache_by_id(node.nd_mid());
                        recv.funcall(getRuby().intern("singleton_method_added"), ((RubyId)node.nd_mid()).toSymbol());
                    }
                    return getRuby().getNil();
                    
                case NODE_UNDEF:
                    if (ruby_class == null) {
                        throw new RubyTypeException("no class to undef method");
                    }
                    ruby_class.undef((RubyId)node.nd_mid());
                    
                    return getRuby().getNil();
                    
                case NODE_ALIAS:
                    if (ruby_class == null) {
                        throw new RubyTypeException("no class to make alias");
                    }
                    ruby_class.aliasMethod((RubyId)node.nd_new(), (RubyId)node.nd_old());
                    // ruby_class.funcall(getRuby().intern("method_added"), ((RubyId)node.nd_mid()).toSymbol());
                    
                    return getRuby().getNil();
                    
                case NODE_VALIAS:
                    //rb_alias_variable(node.nd_new(), node.nd_old());
                    
                    return getRuby().getNil();
                    
                case NODE_CLASS:
                    RubyModule superClass;
                    
                    if (ruby_class == null) {
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
                    if (ruby_class.isConstantDefined((RubyId)node.nd_cname())) {
                        rubyClass = (RubyClass)ruby_class.getConstant((RubyId)node.nd_cname());
                    }
                    if (rubyClass != null) {
                        if (!rubyClass.isClass()) {
                            throw new RubyTypeException(((RubyId)node.nd_cname()).toName() + " is not a class");
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
                                    superClass = getRuby().getObjectClass();
                                }
                                rubyClass = getRuby().defineClassId((RubyId)node.nd_cname(), (RubyClass)superClass);
                                ruby_class.setConstant((RubyId)node.nd_cname(), rubyClass);
                                rubyClass.setClassPath((RubyClass)ruby_class, ((RubyId)node.nd_cname()).toName());
                                // end goto
                            }
                        }
                        if (getRuby().getSecurityLevel() >= 4) {
                            throw new RubySecurityException("extending class prohibited");
                        }
                        // rb_clear_cache();
                    } else {
                        //override_class:
                        if (superClass == null) {
                            superClass = getRuby().getObjectClass();
                        }
                        rubyClass = getRuby().defineClassId((RubyId)node.nd_cname(), (RubyClass)superClass);
                        ruby_class.setConstant((RubyId)node.nd_cname(), rubyClass);
                        rubyClass.setClassPath((RubyClass)ruby_class, ((RubyId)node.nd_cname()).toName());
                    }
                    if (ruby_wrapper != null) {
                        rubyClass.getSingletonClass().includeModule(ruby_wrapper);
                        rubyClass.includeModule(ruby_wrapper);
                    }
                    
                    return setupModule(rubyClass, node.nd_body());
                    
                case NODE_MODULE:
                    if (ruby_class == null) {
                        throw new RubyTypeException("no outer class/module");
                    }
                    
                    RubyModule module = null;
                    
                    if ((ruby_class == getRuby().getObjectClass()) && getRuby().isAutoloadDefined((RubyId)node.nd_cname())) {
                        // getRuby().rb_autoload_load(node.nd_cname());
                    }
                    if (ruby_class.isConstantDefined((RubyId)node.nd_cname())) {
                        module = (RubyModule)ruby_class.getConstant((RubyId)node.nd_cname());
                    }
                    if (module != null) {
                        if (!(module instanceof RubyModule)) {
                            throw new RubyTypeException(((RubyId)node.nd_cname()).toName() + " is not a module");
                            
                        }
                        if (getRuby().getSecurityLevel() >= 4) {
                            throw new RubySecurityException("extending module prohibited");
                        }
                    } else {
                        module = getRuby().defineModuleId((RubyId)node.nd_cname());
                        ruby_class.setConstant((RubyId)node.nd_cname(), module);
                        module.setClassPath(ruby_class, ((RubyId)node.nd_cname()).toName());
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
                    if (getRuby().getSecurityLevel() >= 4 && !rubyClass.isTaint()) {
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
                    // ruby_sourcefile = node.nd_file;
                    // ruby_sourceline = node.nd_nth();
                    // if (trace_func) {
                    //     call_trace_func("line", ruby_sourcefile, ruby_sourceline, self,
                    //     ruby_frame.last_func(),
                    //     ruby_frame.last_class());
                    // }
                    node = node.nd_next();
                    break;
                    
                default:
                   // rom.rb_bug("unknown node type %d", nd_type(node));
            }
        }
    }
    
    private RubyStack classStack = new RubyStack(new LinkedList());
    
    public void pushClass() {
        classStack.push(ruby_class);
    }

    private void popClass() {
        ruby_class = (RubyModule)classStack.pop();
    }

    public NODE ruby_cref = null;
    private NODE top_cref;
    
    private void PUSH_CREF(Object c) {
        ruby_cref = new NODE(NODE_CREF, c, null, ruby_cref);
    }
    
    private void  POP_CREF() {
        ruby_cref = ruby_cref.nd_next();
    }

    
    /** ev_const_get
     *
     */
    protected RubyObject getConstant(NODE cref, RubyId id, RubyObject self) {
        NODE cbase = cref;
        
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

        Frame frame = rubyFrame;
        frame.setTmp(rubyFrame);
        rubyFrame = frame;

        pushClass();
        ruby_class = module;
        getRuby().rubyScope.push();
        RubyVarmap.push(ruby);

        if (node.nd_tbl() != null) {
            List tmp = Collections.nCopies(node.nd_tbl()[0].intValue() + 1, ruby.getNil());
            ShiftableList vars = new ShiftableList(new ArrayList(tmp));
            vars.set(0, (VALUE)node);
            vars.shift(1);
            getRuby().rubyScope.setLocalTbl(node.nd_tbl());
        } else {
            getRuby().rubyScope.setLocalVars(null);
            getRuby().rubyScope.setLocalTbl(null);
        }

        PUSH_CREF(module);
        rubyFrame.setCbase((VALUE)ruby_cref);
        // PUSH_TAG(PROT_NONE);
        
        RubyObject result = null;
        
        // if (( state = EXEC_TAG()) == 0 ) {
            // if (trace_func) {
            //     call_trace_func("class", file, line, ruby_class, 
            //                     ruby_frame->last_func, ruby_frame->last_class );
            // }
            result = eval(ruby_class, node.nd_next());
        // }
            
        // POP_TAG();
        POP_CREF();
        RubyVarmap.pop(ruby);
        getRuby().rubyScope.pop();
        popClass();

        rubyFrame = frame.getTmp();
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
            val = RubyArray.m_newArray(getRuby());
        } else if (!(val instanceof RubyArray)) {
            // if ( rb_respond_to( val, to_ary ) ) {
                // val.funcall(getRuby().intern("to_a"));
            // } else {
                val = RubyArray.m_newArray(getRuby(), val);
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
                assign(self, node.nd_args(), RubyArray.m_newArray(getRuby()), check);
            }
        } else if (check && i < len) {
            // error
        }
        
        while (list != null) {
            i++;
            assign(self, list.nd_head(), getRuby().getNil(), check);
            list = list.nd_next();
        }
        return val;
    }

    /** assign
     *
     */
    public void assign(RubyObject self, NODE lhs, RubyObject val, boolean check) {
        if (val == null) {
            val = getRuby().getNil();
        }
        switch (lhs.nd_type()) {
            case NODE_GASGN:
                ((RubyGlobalEntry)lhs.nd_entry()).set(val);
                break;

            case NODE_IASGN:
                self.setInstanceVar((RubyId)lhs.nd_vid(), val);
                break;

            case NODE_LASGN:
                // if (getRuby().ruby_scope.local_vars == null) {
                //    rb_bug( "unexpected local variable assignment" );
                // }
                getRuby().rubyScope.setLocalVars(lhs.nd_cnt(), val);
                break;

            case NODE_DASGN:
                RubyVarmap.assign(ruby, (RubyId)lhs.nd_vid(), val);
                break;

            case NODE_DASGN_CURR:
                RubyVarmap.assignCurrent(ruby, (RubyId)lhs.nd_vid(), val);
                break;

            case NODE_CDECL:
                ruby_class.setConstant((RubyId)lhs.nd_vid(), val);
                break;

            case NODE_CVDECL:
                if (!ruby_cbase.isSingleton()) {
                    ruby_cbase.declareClassVar((RubyId)lhs.nd_vid(), val);
                    break;
                }
                self = ruby_cbase.getInstanceVar("__attached__");
                /* fall through */
            case NODE_CVASGN:
                self.getClassVarSingleton().setClassVar((RubyId)lhs.nd_vid(), val);
                break;

            case NODE_MASGN:
                massign(self, lhs, val, check);
                break;

            case NODE_CALL:
                RubyObject recv = eval(self, lhs.nd_recv());
                if (lhs.nd_args() == null) {
                    /* attr set */
                    recv.getRubyClass().call(recv, (RubyId)lhs.nd_mid(), new RubyObject[] { val }, 0);
                } else {
                    RubyArray args = (RubyArray)eval(self, lhs.nd_args());
                    args.push(val);
                    recv.getRubyClass().call(recv, (RubyId)lhs.nd_mid(), (RubyObject[])args.getArray().toArray(new RubyObject[0]), 0);
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
        return (scope_vmode & scope) != 0;
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
                args = RubyArray.m_newArray(getRuby(), args);
            }
                
            List argsList = ((RubyArray)args).getArray();
            // ruby_sourcefile = file;
            // ruby_sourceline = line;
            return (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]);
        }
    }
    
    public boolean isBlockGiven() {
        return rubyFrame.getIter() != Iter.ITER_NOT;
    }

    public boolean isFBlockGiven() {
        return (rubyFrame.getPrev() != null) && (rubyFrame.getPrev().getIter() != Iter.ITER_NOT);
    }

    public RubyObject yield0(RubyObject value, RubyObject self, RubyModule klass, boolean acheck) {
        RubyObject result = ruby.getNil();
        
        if (!(isBlockGiven() || isFBlockGiven()) || (ruby_block == null)) {
            throw new RuntimeException("yield called out of block");
        }
        
        RubyVarmap.push(ruby);
        pushClass();
        BLOCK block = ruby_block;
        
        Frame frame = block.frame;
        frame.setPrev(rubyFrame);
        rubyFrame = frame;
        
        VALUE old_cref = ruby_cref;
        ruby_cref = (NODE)rubyFrame.getCbase();
        
        RubyScope oldScope = ruby.rubyScope;
        ruby.rubyScope = block.scope;
        ruby_block = block.prev;
        
        if ((block.flags & BLOCK_D_SCOPE) != 0) {
            dynamicVars = new RubyVarmap(null, null, block.dyna_vars);
        } else {
            dynamicVars = block.dyna_vars;
        }
        
        ruby_class = (klass != null) ? klass : block.klass;
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
        
        ruby_block = block;
        rubyFrame = rubyFrame.getPrev();
        
        ruby_cref = (NODE)old_cref;
        
        // if (ruby_scope->flag & SCOPE_DONT_RECYCLE)
        //    scope_dup(old_scope);
        ruby.rubyScope = oldScope;

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

    private BLOCK BEGIN_CALLARGS() {
        BLOCK tmp_block = ruby_block;
        if (rubyIter.getIter() == Iter.ITER_PRE) {
            ruby_block = ruby_block.prev;
        }
        rubyIter.push(Iter.ITER_NOT);
        return tmp_block;
    }
    
    private void END_CALLARGS(BLOCK tmp_block) {
        ruby_block = tmp_block;
        rubyIter.pop();
    }
    
    // static final int CACHE_SIZE = 0x800;
    // static final int CACHE_MASK = 0x7ff;

    // static long expr1(long c, long m) {
    //    return ((c>>3) ^ m) & CACHE_MASK;
    // }
    
    // cache_entry[CACHE_SIZE] cache;

    // int ruby_in_compile;

    // VALUE ruby_errinfo = ruby.getNil();
    // NODE ruby_eval_tree_begin;
    // NODE ruby_eval_tree;
    // int ruby_nerrs;

    // VALUE rb_eLocalJumpError;
    // VALUE rb_eSysStackError;

    // VALUE ruby_top_self;

    private static final int BLOCK_D_SCOPE  = 1;
    private static final int BLOCK_DYNAMIC  = 2;
    private static final int BLOCK_ORPHAN   = 4;
    
    BLOCK ruby_block = new BLOCK();

    private BLOCK PUSH_BLOCK(NODE v, NODE b, VALUE self) {
        BLOCK _block = new BLOCK();
        // _block.tag = new_blktag();
        _block.var = v;
        _block.body = b;
        _block.self = self;
        _block.frame = rubyFrame;
        _block.klass = ruby_class;
    //    _block.frame.file = ruby_sourcefile;
    //    _block.frame.line = ruby_sourceline;
        _block.scope = getRuby().rubyScope;
        _block.prev = ruby_block;
        _block.iter = rubyIter.getIter();
        _block.vmode = scope_vmode;
        _block.flags = BLOCK_D_SCOPE;
        _block.dyna_vars = getDynamicVars();
        ruby_block = _block;
        return _block;
    }

    private void POP_BLOCK_TAG(BLOCKTAG tag) { 
         if ((tag.flags & BLOCK_DYNAMIC) != 0) {
            tag.flags |= BLOCK_ORPHAN;
         } else {
            // rb_gc_force_recycle((VALUE)tag);
         }
    }

    private void POP_BLOCK(BLOCK _block) {
        POP_BLOCK_TAG(_block.tag);
        ruby_block = _block.prev;
    }
    
    /** Getter for property dynamicVars.
     * @return Value of property dynamicVars.
     */
    public RubyVarmap getDynamicVars() {
        return dynamicVars;
    }
    
    /** Setter for property dynamicVars.
     * @param dynamicVars New value of property dynamicVars.
     */
    public void setDynamicVars(RubyVarmap dynamicVars) {
        this.dynamicVars = dynamicVars;
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
    
}

// C structs

// class cache_entry {  /* method hash table. */
//     ID mid;   /* method's id */
//     ID mid0;   /* method's original id */
//     VALUE klass;  /* receiver's class */
//     VALUE origin;  /* where method defined  */
//     NODE method;
//     int noex;
// }

class BLOCKTAG {
    RubyObject _super;
    long dst;
    long flags;
}

class BLOCK {
    NODE var;
    NODE body;
    VALUE self;
    Frame frame;
    RubyScope scope;
    BLOCKTAG tag;
    RubyModule klass;
    int iter;
    int vmode;
    int flags;
    RubyVarmap dyna_vars;
    VALUE orig_thread;
    BLOCK prev;
}