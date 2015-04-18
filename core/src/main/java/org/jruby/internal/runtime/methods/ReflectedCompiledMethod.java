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
 * Copyright (c) 2007 Peter Brant <peter.brant@gmail.com>
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
package org.jruby.internal.runtime.methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectedCompiledMethod extends CompiledMethod {
    private final Method method;
    
    public ReflectedCompiledMethod(RubyModule implementationClass,
            Visibility visibility, StaticScope staticScope, Object scriptObject, Method method, CallConfiguration callConfig, ISourcePosition position, String parameterDesc) {
        super(null);
        init(implementationClass, visibility, staticScope, scriptObject, callConfig, position, parameterDesc);
        
        this.method = method;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject[] args, Block block) {
        callConfig.pre(context, self, getImplementationClass(), name, block, staticScope);

        Ruby runtime = context.runtime;
        int callNumber = context.callNumber;
        try {
            boolean isTrace = runtime.hasEventHooks();
            try {
                if (isTrace) {
                    runtime.callEventHooks(context, RubyEvent.CALL, position.getFile(), position.getLine(), name, getImplementationClass());
                }
                return (IRubyObject)method.invoke(null, $scriptObject, context, self, args, block);
            } finally {
                if (isTrace) {
                    runtime.callEventHooks(context, RubyEvent.RETURN, context.getFile(), context.getLine(), name, getImplementationClass());
                }
            }
            
        } catch (IllegalArgumentException e) {
            throw RaiseException.createNativeRaiseException(runtime, e, method);
        } catch (IllegalAccessException e) {
            throw RaiseException.createNativeRaiseException(runtime, e, method);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof JumpException.ReturnJump) {
                return handleReturn(context, (JumpException.ReturnJump)cause, callNumber);
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
