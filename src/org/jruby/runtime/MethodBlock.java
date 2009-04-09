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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.runtime;

import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public abstract class MethodBlock extends BlockBody {
    private final RubyMethod method;
    
    private final Arity arity;
     
    // This is a dummy scope; we should find a way to make that more explicit
    private StaticScope staticScope;

    public static Block createMethodBlock(ThreadContext context, IRubyObject self, DynamicScope dynamicScope, MethodBlock body) {
        Binding binding = context.currentBinding(self, dynamicScope);
        return new Block(body, binding);
    }

    public MethodBlock(RubyMethod method, StaticScope staticScope) {
        super(BlockBody.SINGLE_RESTARG);
        this.method = method;
        this.arity = Arity.createArity((int) method.arity().getLongValue());
        this.staticScope = staticScope;
    }

    public abstract IRubyObject callback(IRubyObject value, IRubyObject method, IRubyObject self, Block block);

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true, binding, type);
    }
    
    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldNoScope(binding, klass);
    }
    
    protected void post(ThreadContext context, Binding binding, Frame lastFrame) {
        context.postYieldNoScope(lastFrame);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yield(context, null, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yield(context, arg0, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yield(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1), null, null, true, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yield(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1, arg2), null, null, true, binding, type);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        return yield(context, value, null, null, false, binding, type);
    }

    /**
     * Yield to this block, usually passed to the current call.
     * 
     * @param context represents the current thread-specific data
     * @param value The value to yield, either a single value or an array of values
     * @param self The current self
     * @param klass
     * @param aValue Should value be arrayified or not?
     * @return
     */
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, 
            RubyModule klass, boolean aValue, Binding binding, Block.Type type) {
        if (klass == null) {
            self = binding.getSelf();
            binding.getFrame().setSelf(self);
        }
        
        Frame lastFrame = pre(context, klass, binding);

        try {
            // This while loop is for restarting the block call in case a 'redo' fires.
            while (true) {
                try {
                    return callback(value, method, self, Block.NULL_BLOCK);
                } catch (JumpException.RedoJump rj) {
                    context.pollThreadEvents();
                    // do nothing, allow loop to redo
                } catch (JumpException.BreakJump bj) {
                    if (bj.getTarget() == null) {
                        bj.setTarget(this);                            
                    }                        
                    throw bj;
                }
            }
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return (IRubyObject)nj.getValue();
        } finally {
            post(context, binding, lastFrame);
        }
    }
    
    public StaticScope getStaticScope() {
        // TODO: This is actually now returning the scope of whoever called Method#to_proc
        // which is obviously wrong; but there's no scope to provide for many methods.
        // It fixes JRUBY-2237, but needs a better solution.
        return staticScope;
    }

    public void setStaticScope(StaticScope newScope) {
        this.staticScope = newScope;
    }

    public Block cloneBlock(Binding binding) {
        binding = binding.clone();

        return new Block(this, binding);
    }

    /**
     * What is the arity of this block?
     * 
     * @return the arity
     */
    public Arity arity() {
        return arity;
    }
}
