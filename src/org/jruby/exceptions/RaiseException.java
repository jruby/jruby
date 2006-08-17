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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jruby.IRuby;
import org.jruby.NativeException;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseException extends JumpException {
	private static final long serialVersionUID = -7612079169559973951L;
	
	private RubyException exception;

    public RaiseException(RubyException actException) {
    	super(JumpType.RaiseJump);
        setException(actException, false);
    }

    public RaiseException(IRuby runtime, RubyClass excptnClass, String msg, boolean nativeException) {
		super(msg, JumpType.RaiseJump);
        if (msg == null) {
            msg = "No message available";
        }
        setException((RubyException) excptnClass.callMethod("new", excptnClass.getRuntime().newString(msg)), nativeException);
    }
    
    public static RaiseException createNativeRaiseException(IRuby runtime, Throwable cause) {
        NativeException nativeException = new NativeException(runtime, runtime.getClass(NativeException.CLASS_NAME), cause);
        return new RaiseException(cause, nativeException);
    }

    private static String buildMessage(Throwable exception) {
	    StringBuffer sb = new StringBuffer();
	    StringWriter stackTrace = new StringWriter();
	    exception.printStackTrace(new PrintWriter(stackTrace));
	
	    sb.append("Native Exception: '").append(exception.getClass()).append("'; ");
	    sb.append("Message: ").append(exception.getMessage()).append("; ");
	    sb.append("StackTrace: ").append(stackTrace.getBuffer().toString());

	    return sb.toString();
    }

    public RaiseException(Throwable cause, NativeException nativeException) {
        super(buildMessage(cause), cause, JumpType.RaiseJump);
        setException(nativeException, false);
    }

    /**
     * Gets the exception
     * @return Returns a RubyException
     */
    public RubyException getException() {
        return exception;
    }

    /**
     * Sets the exception
     * @param newException The exception to set
     */
    protected void setException(RubyException newException, boolean nativeException) {
        IRuby runtime = newException.getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        
        runtime.getGlobalVariables().set("$!", newException);

        if (runtime.getTraceFunction() != null) {
            runtime.callTraceFunction(
                "return",
                tc.getPosition(),
                tc.getFrameSelf(),
                tc.getFrameLastFunc(),
                tc.getFrameLastClass());
        }

        this.exception = newException;

        if (runtime.getStackTraces() > 5) {
            return;
        }

        runtime.setStackTraces(runtime.getStackTraces() + 1);

        if (newException.callMethod("backtrace").isNil() && tc.getSourceFile() != null) {
            IRubyObject backtrace = tc.createBacktrace(0, nativeException);
            newException.callMethod("set_backtrace", backtrace);
        }

        runtime.setStackTraces(runtime.getStackTraces() - 1);
    }
}
