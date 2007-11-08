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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
public class CompiledBlock extends Block {
    private CompiledBlockCallback callback;
    private boolean hasMultipleArgsHead;
    private int argumentType;
    private boolean light;

    public CompiledBlock(ThreadContext context, IRubyObject self, Arity arity, DynamicScope dynamicScope,
            CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        this(self,
             context.getCurrentFrame(),
                Visibility.PUBLIC,
                context.getRubyClass(),
                dynamicScope, arity, callback, hasMultipleArgsHead, argumentType);
    }

    private CompiledBlock(IRubyObject self, Frame frame, Visibility visibility, RubyModule klass,
        DynamicScope dynamicScope, Arity arity, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        super(null, self, arity, frame, visibility, klass, dynamicScope);
        this.callback = callback;
        this.hasMultipleArgsHead = hasMultipleArgsHead;
        this.argumentType = argumentType;
    }
    
    public void setLight(boolean light) {
        this.light = light;
    }
    
    public boolean getLight() {
        return light;
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        switch (type) {
        case NORMAL: {
            assert false : "can this happen?";
            if (args.length == 1 && args[0] instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT || argumentType == SINGLE_RESTARG) {
                    args = ((RubyArray) args[0]).toJavaArray();
                }
                break;
            }
        }
        case PROC: {
            if (args.length == 1 && args[0] instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT && argumentType != SINGLE_RESTARG) {
                    args = ((RubyArray) args[0]).toJavaArray();
                }
            }
            break;
        }
        case LAMBDA:
            arity().checkArity(context.getRuntime(), args);
            break;
        }

        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue) {
        if (klass == null) {
            self = this.self;
            frame.setSelf(self);
        }

        // handle as though it's just an array coming in...i.e. it should be multiassigned or just 
        // assigned as is to var 0.
        // FIXME for now, since masgn isn't supported, this just wraps args in an IRubyObject[], 
        // since single vars will want that anyway
        Visibility oldVis = frame.getVisibility();
        try {
            IRubyObject[] realArgs = aValue ? 
                    setupBlockArgs(context, args, self) : setupBlockArg(context, args, self); 
            pre(context, klass);
            
            // NOTE: Redo jump handling is within compiled closure, wrapping the body
            try {
                return callback.call(context, self, realArgs);
            } catch (JumpException.BreakJump bj) {
                if (bj.getTarget() == null) {
                    bj.setTarget(this);
                }
                throw bj;
            }
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return type == Type.LAMBDA ? context.getRuntime().getNil() : (IRubyObject)nj.getValue();
        } finally {
            frame.setVisibility(oldVis);
            post(context);
        }
    }
    
    protected void pre(ThreadContext context, RubyModule klass) {
        if (light) {
            context.preYieldLightBlock(this, klass);
        } else {
            context.preYieldSpecificBlock(this, klass);
        }
    }
    
    protected void post(ThreadContext context) {
        if (light) {
            context.postYieldLight(this);
        } else {
            context.postYield(this);
        }
    }

    private IRubyObject[] setupBlockArgs(ThreadContext context, IRubyObject value, IRubyObject self) {
        switch (argumentType) {
        case ZERO_ARGS:
            return IRubyObject.NULL_ARRAY;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return new IRubyObject[] {value};
        default:
            int length = arrayLength(value);
            switch (length) {
            case 0:
                value = context.getRuntime().getNil();
                break;
            case 1:
                value = ((RubyArray)value).eltInternal(0);
                break;
            default:
                context.getRuntime().getWarnings().warn("multiple values for a block parameter (" +
                        length + " for 1)");
            }
            return new IRubyObject[] {value};
        }
    }

    private IRubyObject[] setupBlockArg(ThreadContext context, IRubyObject value, IRubyObject self) {
        switch (argumentType) {
        case ZERO_ARGS:
            return IRubyObject.NULL_ARRAY;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return new IRubyObject[] {ArgsUtil.convertToRubyArray(context.getRuntime(), value, hasMultipleArgsHead)};
        default:
        // FIXME: the test below would be enabled if we could avoid processing block args for the cases where we don't have any args
        // since we can't do that just yet, it's disabled
            if (value == null) {
                context.getRuntime().getWarnings().warn("multiple values for a block parameter (0 for 1)");
                return new IRubyObject[] {context.getRuntime().getNil()};
            }
            return new IRubyObject[] {value};
        }
    }

    public Block cloneBlock() {
        Block newBlock = new CompiledBlock(
                self,
                frame.duplicate(),
                visibility,
                klass,
                light ? dynamicScope : dynamicScope.cloneScope(),
                arity,
                callback,
                hasMultipleArgsHead,
                argumentType);
        
        newBlock.type = type;
        
        return newBlock;
    }
}
