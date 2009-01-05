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
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.ast.executable.RubiniusMachine;
import org.jruby.ast.executable.RubiniusCMethod;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.RubyEvent;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubiniusMethod extends DynamicMethod implements JumpTarget {
    private RubiniusCMethod cmethod;
    private StaticScope staticScope;
    private Arity arity;

    public RubiniusMethod(RubyModule implementationClass, RubiniusCMethod cmethod, StaticScope staticScope, Visibility visibility) {
        super(implementationClass, visibility, CallConfiguration.FrameFullScopeFull);
        this.staticScope = staticScope;
        this.cmethod = cmethod;
        this.arity = Arity.optional();
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
    	assert args != null;
        //        System.err.println("--- entering " + cmethod.name);
        Ruby runtime = context.getRuntime();
        
        callConfig.pre(context, self, klazz, name, block, staticScope, this);
        
        try {
            if (runtime.hasEventHooks()) {
                traceCall(context, runtime, name);
            }

            return RubiniusMachine.INSTANCE.exec(context, self, cmethod.code, cmethod.literals, args);
        } catch (JumpException.ReturnJump rj) {
        	if (rj.getTarget() == this) return (IRubyObject) rj.getValue();

            
       		throw rj;
        } finally {
            if (runtime.hasEventHooks()) {
                traceReturn(context, runtime, name);
            }
            
            callConfig.post(context);
            //              System.err.println("--- returning from " + cmethod.name);
        }
    }

    private void traceReturn(ThreadContext context, Ruby runtime, String name) {
        if (!runtime.hasEventHooks()) {
            return;
        }
        
        Frame frame = context.getPreviousFrame();

        runtime.callEventHooks(context, RubyEvent.RETURN, frame.getFile(), frame.getLine(), name, getImplementationClass());
    }
    
    private void traceCall(ThreadContext context, Ruby runtime, String name) {
        if (!runtime.hasEventHooks()) {
            return;
        }
        
        runtime.callEventHooks(context, RubyEvent.CALL, context.getFile(), context.getLine(), name, getImplementationClass());
    }
    
    public Arity getArity() {
        return this.arity;
    }
    
    public DynamicMethod dup() {
        return new RubiniusMethod(getImplementationClass(), cmethod, staticScope, getVisibility());
    }	
}// RubiniusMethod
