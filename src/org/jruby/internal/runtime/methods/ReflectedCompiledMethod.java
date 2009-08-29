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
 * Copyright (c) 2007 Peter Brant <peter.brant@gmail.com>
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
package org.jruby.internal.runtime.methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectedCompiledMethod extends CompiledMethod {
    private final Method method;
    
    public ReflectedCompiledMethod(RubyModule implementationClass, Arity arity,
            Visibility visibility, StaticScope staticScope, Object scriptObject, Method method, CallConfiguration callConfig) {
        super();
        init(implementationClass, arity, visibility, staticScope, scriptObject, callConfig);
        
        this.method = method;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject[] args, Block block) {
        callConfig.pre(context, self, getImplementationClass(), name, block, staticScope, this);
        
        Ruby runtime = context.getRuntime();
        try {
            boolean isTrace = runtime.hasEventHooks();
            try {
                if (isTrace) {
                    // XXX Wrong, but will have to do for now
                    runtime.callEventHooks(context, RubyEvent.CALL, context.getFile(), context.getLine(), name, getImplementationClass());
                }
                return (IRubyObject)method.invoke(null, $scriptObject, context, self, args, block);
            } finally {
                if (isTrace) {
                    Frame frame = context.getPreviousFrame();

                    runtime.callEventHooks(context, RubyEvent.RETURN, frame.getFile(), frame.getLine(), name, getImplementationClass());
                }
            }
            
        } catch (IllegalArgumentException e) {
            throw RaiseException.createNativeRaiseException(runtime, e, method);
        } catch (IllegalAccessException e) {
            throw RaiseException.createNativeRaiseException(runtime, e, method);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof JumpException.ReturnJump) {
                return handleReturn(context, (JumpException.ReturnJump)cause);
            } else if (cause instanceof JumpException.RedoJump) {
                return handleRedo(runtime);
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else if (cause instanceof Error) {
                throw (Error)cause;                
            } else {
                throw RaiseException.createNativeRaiseException(runtime, cause, method);
            }
        } finally {
            callConfig.post(context);
        }
    }
}
