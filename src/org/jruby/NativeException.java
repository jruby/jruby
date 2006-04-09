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
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
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

import java.io.PrintStream;

import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;


public class NativeException extends RubyException {

    private final Throwable cause;
    public static final String CLASS_NAME = "NativeException";
	private final IRuby runtime;

    public NativeException(IRuby runtime, RubyClass rubyClass, Throwable cause) {
        super(runtime, rubyClass, cause.getClass().getName()+": "+cause.getMessage());
		this.runtime = runtime;
        this.cause = cause;
    }
    
    public static RubyClass createClass(IRuby runtime, RubyClass baseClass) {
    	RubyClass exceptionClass = runtime.defineClass(CLASS_NAME, baseClass);
    	
		CallbackFactory callbackFactory = runtime.callbackFactory(NativeException.class);
		exceptionClass.defineMethod("cause", 
				callbackFactory.getMethod("cause"));		

		return exceptionClass;
    }
    
    public IRubyObject cause() {
        return JavaObject.wrap(getRuntime(), cause);
    }
    
    public IRubyObject backtrace() {
        IRubyObject rubyTrace = super.backtrace();
        if (rubyTrace.isNil())
            return rubyTrace;
        RubyArray array = (RubyArray) rubyTrace;
        StackTraceElement[] stackTrace = cause.getStackTrace();
        for (int i=stackTrace.length-1; i>=0; i--) {
            StackTraceElement element = stackTrace[i];
            String line = element.toString();
            RubyString string = runtime.newString(line);
            array.unshift(string);
        }
        return rubyTrace;
    }
    
    public void printBacktrace(PrintStream errorStream) {
    	super.printBacktrace(errorStream);
    	errorStream.println("Complete Java stackTrace");
    	cause.printStackTrace(errorStream);
    }
}
