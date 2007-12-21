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
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
public class CompiledBlock extends BlockBody {
    protected final CompiledBlockCallback callback;
    protected final boolean hasMultipleArgsHead;
    protected final int argumentType;
    protected final Arity arity;
    protected final StaticScope scope;
    
    public static Block newCompiledClosure(IRubyObject self, Frame frame, Visibility visibility, RubyModule klass,
        DynamicScope dynamicScope, Arity arity, StaticScope scope, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        Binding binding = new Binding(self, frame, visibility, klass, dynamicScope);
        BlockBody body = new CompiledBlock(arity, scope, callback, hasMultipleArgsHead, argumentType);
        
        return new Block(body, binding);
    }
    
    public static Block newCompiledClosure(ThreadContext context, IRubyObject self, Arity arity,
            StaticScope scope, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        Frame f = context.getCurrentFrame();
        f.setPosition(context.getPosition());
        return newCompiledClosure(
                self,
                f,
                Visibility.PUBLIC,
                context.getRubyClass(),
                context.getCurrentScope(),
                arity,
                scope,
                callback,
                hasMultipleArgsHead,
                argumentType);
    }

    protected CompiledBlock(Arity arity, StaticScope scope, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        this.arity = arity;
        this.scope = scope;
        this.callback = callback;
        this.hasMultipleArgsHead = hasMultipleArgsHead;
        this.argumentType = argumentType;
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
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

        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true, binding, type);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        return yield(context, value, null, null, false, binding, type);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Block.Type type) {
        if (klass == null) {
            self = binding.getSelf();
            binding.getFrame().setSelf(self);
        }

        // handle as though it's just an array coming in...i.e. it should be multiassigned or just 
        // assigned as is to var 0.
        // FIXME for now, since masgn isn't supported, this just wraps args in an IRubyObject[], 
        // since single vars will want that anyway
        Visibility oldVis = binding.getFrame().getVisibility();
        IRubyObject[] realArgs = aValue ? 
                setupBlockArgs(context, args, self) : setupBlockArg(context, args, self); 
        pre(context, klass, binding);
        
        try {
            return callback.call(context, self, realArgs);
        } catch (JumpException.BreakJump bj) {
            if (bj.getTarget() == null) {
                bj.setTarget(this);
            }
            throw bj;
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return type == Block.Type.LAMBDA ? context.getRuntime().getNil() : (IRubyObject)nj.getValue();
        } finally {
            binding.getFrame().setVisibility(oldVis);
            post(context, binding);
        }
    }
    
    protected void pre(ThreadContext context, RubyModule klass, Binding binding) {
        context.preYieldSpecificBlockNEW(binding, scope, klass);
    }
    
    protected void post(ThreadContext context, Binding binding) {
        context.postYield(binding);
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
            if (value == null) {
                context.getRuntime().getWarnings().warn("multiple values for a block parameter (0 for 1)");
                return new IRubyObject[] {context.getRuntime().getNil()};
            }
            return new IRubyObject[] {value};
        }
    }
    
    public StaticScope getStaticScope() {
        return scope;
    }

    public Block cloneBlock(Binding binding) {
        binding = new Binding(binding.getSelf(),
                binding.getFrame().duplicate(),
                binding.getVisibility(),
                binding.getKlass(),
                binding.getDynamicScope());
        
        return new Block(this, binding);
    }

    @Override
    public Arity arity() {
        return arity;
    }
}
