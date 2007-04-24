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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
public class CallBlock extends Block {
    private Arity arity;
    private BlockCallback callback;
    private IRubyObject self;
    private RubyModule imClass;
    private ThreadContext tc;

    public CallBlock(IRubyObject self, RubyModule imClass, Arity arity, BlockCallback callback, ThreadContext ctx) {
        super(null,
                self,
                ctx.getCurrentFrame(),
                ctx.peekCRef(),
                Visibility.PUBLIC,
                ctx.getRubyClass(),
                ctx.getCurrentScope());
        this.arity = arity;
        this.callback = callback;
        this.self = self;
        this.imClass = imClass;
        this.tc = ctx;
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return callback.call(context, args, Block.NULL_BLOCK);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value) {
        return yield(context, value, null, null, false);
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
            RubyModule klass, boolean aValue) {
        if (klass == null) {
            self = this.self;
            frame.setSelf(self);
        }
        
        pre(context, klass);

        try {
            IRubyObject[] args = new IRubyObject[] {value};
            // This while loop is for restarting the block call in case a 'redo' fires.
            while (true) {
                try {
                    return callback.call(context, args, NULL_BLOCK);
                } catch (JumpException je) {
                    if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                        context.pollThreadEvents();
                        // do nothing, allow loop to redo
                    } else {
                        if (je.getJumpType() == JumpException.JumpType.BreakJump && je.getTarget() == null) {
                            je.setTarget(this);                            
                        }                        
                        throw je;
                    }
                }
            }
            
        } catch (JumpException je) {
            // A 'next' is like a local return from the block, ending this call or yield.
        	if (je.getJumpType() == JumpException.JumpType.NextJump) return (IRubyObject) je.getValue();

            throw je;
        } finally {
            post(context);
        }
    }

    public Block cloneBlock() {
        return new CallBlock(self,imClass,arity,callback,tc);
    }

    public Arity arity() {
        return arity;
    }
}
