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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
package org.jruby;

import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.IterateCallable;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * The RubyMethod class represents a RubyMethod object.
 * 
 * You can get such a method by calling the "method" method of an object.
 * 
 * Note: This was renamed from Method.java
 * 
 * @author  jpetersen
 * @since 0.2.3
 */
public class RubyMethod extends RubyObject {
    protected RubyModule implementationModule;
    protected String methodName;
    protected RubyModule originModule;
    protected String originName;
    protected ICallable method;
    protected IRubyObject receiver;

    protected RubyMethod(IRuby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    /** Create the RubyMethod class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(IRuby runtime) {
		RubyClass methodClass = runtime.defineClass("Method", runtime.getObject());
    	
		CallbackFactory callbackFactory = runtime.callbackFactory(RubyMethod.class);
        
		methodClass.defineMethod("arity", 
				callbackFactory.getMethod("arity"));
		methodClass.defineMethod("to_proc", 
				callbackFactory.getMethod("to_proc"));
		methodClass.defineMethod("unbind", 
				callbackFactory.getMethod("unbind"));
		methodClass.defineMethod("call", 
				callbackFactory.getOptMethod("call"));
		methodClass.defineMethod("[]", 
				callbackFactory.getOptMethod("call"));

		return methodClass;
    }

    public static RubyMethod newMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        ICallable method,
        IRubyObject receiver) {
        IRuby runtime = implementationModule.getRuntime();
        RubyMethod newMethod = new RubyMethod(runtime, runtime.getClass("Method"));

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.method = method;
        newMethod.receiver = receiver;

        return newMethod;
    }

    /** Call the method.
     * 
     */
    public IRubyObject call(IRubyObject[] args) {
    	assert args != null;

    	method.getArity().checkArity(getRuntime(), args);
        
        getRuntime().getCurrentContext().pushIter(getRuntime().getCurrentContext().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
        try {
            // FIXME: should lastClass be implementation module for a Method?
            return method.call(getRuntime(), receiver, implementationModule, methodName, args, false);
        } finally {
            getRuntime().getCurrentContext().popIter();
        }
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    public RubyFixnum arity() {
        return getRuntime().newFixnum(method.getArity().getValue());
    }

    /** Create a Proc object.
     * 
     */
    public IRubyObject to_proc() {
    	CallbackFactory f = getRuntime().callbackFactory(RubyMethod.class);
		IRuby r = getRuntime();
        r.getCurrentContext().preToProc(Block.createBlock(null, new IterateCallable(f.getBlockMethod("bmcall"), this), r.getTopSelf()));
		
		try {
		    while (true) {
		        try {
		            return f.getSingletonMethod("mproc").execute(getRuntime().getNil(), IRubyObject.NULL_ARRAY);
		        } catch (JumpException je) {
		        	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
		                IRubyObject breakValue = (IRubyObject)je.getPrimaryData();
		                
		                return breakValue == null ? r.getNil() : breakValue;
		        	} else if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
		        		return (IRubyObject)je.getPrimaryData();
		        	} else if (je.getJumpType() == JumpException.JumpType.RetryJump) {
		        		// Execute iterateMethod again.
		        	} else {
		        		throw je;
		        	}
		        }
		    }
		} finally {
            r.getCurrentContext().postToProc();
		}
    }

    /** Create a Proc object which is called like a ruby method.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject mproc(IRubyObject recv) {
    	IRuby runtime = recv.getRuntime();

        runtime.getCurrentContext().preMproc();
        
        try {
            return RubyKernel.proc(recv);
        } finally {
            runtime.getCurrentContext().postMproc();
        }
    }

    /** Delegate a block call to a bound method call.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject bmcall(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (blockArg instanceof RubyArray) {
            return ((RubyMethod) arg1).call(((RubyArray) blockArg).toJavaArray());
        }
        return ((RubyMethod) arg1).call(new IRubyObject[] { blockArg });
    }

    public RubyUnboundMethod unbind() {
        RubyUnboundMethod unboundMethod =
        	RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, method);
        unboundMethod.receiver = this;
        unboundMethod.infectBy(this);
        
        return unboundMethod;
    }
}

