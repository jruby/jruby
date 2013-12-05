/***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public abstract class MethodBlock extends ContextAwareBlockBody {
    private final RubyMethod method;
    private final String filename;
    private final int line;
    
    public static Block createMethodBlock(ThreadContext context, IRubyObject self, DynamicScope dynamicScope, MethodBlock body) {
        RubyMethod method = body.method;
        RubyModule module = method.getMethod().getImplementationClass();
        Frame frame = new Frame();

        frame.setKlazz(module);
        frame.setName(method.getMethodName());
        frame.setSelf(method.receiver(context));
        frame.setVisibility(method.getMethod().getVisibility());
        
        Binding binding = new Binding(
                frame,
                module,
                dynamicScope,
                new BacktraceElement(method.getMethodName(), body.getFile(), body.getLine()));

        return new Block(body, binding);
    }

    public MethodBlock(RubyMethod method, StaticScope staticScope) {
        super(staticScope, Arity.createArity((int) method.arity().getLongValue()), BlockBody.SINGLE_RESTARG);
        
        this.method = method;
        String filename = method.getFilename();
        if (filename == null) filename = "(method)";
        this.filename = filename;
        this.line = method.getLine();
    }

    public abstract IRubyObject callback(IRubyObject value, IRubyObject method, IRubyObject self, Block block);

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        return yield(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type, Block block) {
        return yield(context, args, null, null, binding, type, block);
    }
    
    @Override
    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldNoScope(binding, klass);
    }
    
    @Override
    protected void post(ThreadContext context, Binding binding, Visibility visibility, Frame lastFrame) {
        context.postYieldNoScope(lastFrame);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yield(context, null, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yield(context, arg0, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yield(context, new IRubyObject[] { arg0, arg1 }, null, null, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yield(context, new IRubyObject[] { arg0, arg1, arg2 }, null, null, binding, type);
    }
    
    @Override
    protected IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        return yield(context, value, binding, type);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type, Block block) {
        return yield(context, value, binding, type);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self,
                             RubyModule klass, Binding binding, Block.Type type) {
        return yield(context, args, self, klass, binding, type, Block.NULL_BLOCK);
    }

    /**
     * Yield to this block, usually passed to the current call.
     * 
     * @param context represents the current thread-specific data
     * @param args The args for yield
     * @param self The current self
     * @param klass
     * @return
     */
    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self,
            RubyModule klass, Binding binding, Block.Type type, Block block) {
        if (klass == null) {
            self = binding.getSelf();
            binding.getFrame().setSelf(self);
        }
        
        Frame lastFrame = pre(context, klass, binding);

        try {
            // This while loop is for restarting the block call in case a 'redo' fires.
            while (true) {
                try {
                    IRubyObject[] preppedArgs = RubyProc.prepareArgs(context, type, arity, args);
                    return callback(context.runtime.newArrayNoCopyLight(preppedArgs), method, self, block);
                } catch (JumpException.RedoJump rj) {
                    context.pollThreadEvents();
                    // do nothing, allow loop to redo
                } catch (JumpException.BreakJump bj) {
//                    if (bj.getTarget() == 0) {
//                        bj.setTarget(this);
//                    }                  
                    throw bj;
                }
            }
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return (IRubyObject)nj.getValue();
        } finally {
            post(context, binding, null, lastFrame);
        }
    }

    public String getFile() {
        return filename;
    }

    public int getLine() {
        return line;
    }

    public RubyMethod getMethod() {
        return method;
    }
}
