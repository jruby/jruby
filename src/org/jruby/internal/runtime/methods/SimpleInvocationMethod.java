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
package org.jruby.internal.runtime.methods;

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.exceptions.MainExitException;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class SimpleInvocationMethod extends AbstractMethod {
    private Arity arity;
    public SimpleInvocationMethod(RubyModule implementationClass, Arity arity, Visibility visibility) {
    	super(implementationClass, visibility);
        this.arity = arity;
    }

    public void preMethod(ThreadContext context, RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper, Block block) {
    }
    
    public void postMethod(ThreadContext context) {
    }
    
    public IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper, Block block) {
        assert false;
        return null;
    }
    
    private IRubyObject wrap(IRuby runtime, IRubyObject receiver, IRubyObject[] args) {
        try {
            return call(receiver,args);
        } catch(RaiseException e) {
            throw e;
        } catch(JumpException e) {
            throw e;
        } catch(ThreadKill e) {
            throw e;
        } catch(MainExitException e) {
            throw e;
        } catch(Exception e) {
            runtime.getJavaSupport().handleNativeException(e);
            return runtime.getNil();
        }        
    }

    public IRubyObject call(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper, Block block) {
        IRuby runtime = context.getRuntime();
        arity.checkArity(runtime, args);

        if(runtime.getTraceFunction() != null) {
            ISourcePosition position = context.getPreviousFramePosition();

            runtime.callTraceFunction(context, "c-call", position, receiver, name, getImplementationClass());
            try {
                return wrap(runtime,receiver,args);
            } finally {
                runtime.callTraceFunction(context, "c-return", position, receiver, name, getImplementationClass());
            }
        }
        return wrap(runtime,receiver,args);
    }

    public abstract IRubyObject call(IRubyObject receiver, IRubyObject[] args);
    
    public DynamicMethod dup() {
        System.err.println("shouldn't dup this class either...");
        return null;
    }

    public Arity getArity() {
        return arity;
    }
}// SimpleInvocationMethod
