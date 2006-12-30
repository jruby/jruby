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
 * Copyright (C) 2006 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.CallbackFactory;

/**
 * @author Anders Bengtsson
 */
public class RubyNameError extends RubyException {
    private IRubyObject name;

    public static RubyClass createNameErrorClass(IRuby runtime, RubyClass standardErrorClass) {
        RubyClass nameErrorClass = runtime.defineClass("NameError", standardErrorClass);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyNameError.class);
        
        nameErrorClass.defineFastSingletonMethod("new", 
                callbackFactory.getOptSingletonMethod("newRubyNameError"));		
        nameErrorClass.defineFastSingletonMethod("exception", 
				callbackFactory.getOptSingletonMethod("newRubyNameError"));		

        nameErrorClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        nameErrorClass.defineFastMethod("name", callbackFactory.getMethod("name"));

        return nameErrorClass;
    }

    protected RubyNameError(IRuby runtime, RubyClass exceptionClass) {
        this(runtime, exceptionClass, exceptionClass.getName().toString());
    }

    public RubyNameError(IRuby runtime, RubyClass exceptionClass, String message) {
        this(runtime, exceptionClass, message, null);
    }

    public RubyNameError(IRuby runtime, RubyClass exceptionClass, String message, String name) {
        super(runtime, exceptionClass, message);
        this.name = name == null ? runtime.getNil() : runtime.newString(name);
    }
    
    public static RubyNameError newRubyNameError(IRubyObject recv, IRubyObject[] args) {
        IRuby runtime = recv.getRuntime();
        RubyNameError newError = new RubyNameError(runtime, (RubyClass)recv);
        newError.callInit(args);
        return newError;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        super.initialize(args);
        if (args.length > 1) {
            name = args[1];
        }
        return this;
    }

    public IRubyObject name() {
        return name;
    }
}
