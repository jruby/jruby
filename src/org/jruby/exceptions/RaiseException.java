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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.Frame;
import org.jruby.runtime.FrameStack;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RaiseException extends JumpException {
    private RubyException exception;

    public RaiseException(RubyException actException) {
        setException(actException, false);
    }

    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, boolean nativeException) {
		super(msg);
        setException(RubyException.newException(runtime, excptnClass, msg), nativeException);
    }

    public RaiseException(Ruby runtime, String excptnClassName, String msg) {
		super(msg);
        RubyClass excptnClass = runtime.getClass(excptnClassName);
        if (excptnClass == null) {
            System.err.println(excptnClassName);
        }
        setException(RubyException.newException(runtime, excptnClass, msg), false);
    }

    /** 
     * Create an Array with backtrace information.
     * @param runtime
     * @param level
     * @param nativeException
     * @return an Array with the backtrace 
     */
    public static IRubyObject createBacktrace(Ruby runtime, int level, boolean nativeException) {
        RubyArray backtrace = runtime.newArray();
        FrameStack stack = runtime.getFrameStack();
        int traceSize = stack.size() - level - 1;
        
        if (traceSize <= 0) {
        	return backtrace;
        }
        
        if (nativeException) {
            // assert level == 0;
            addBackTraceElement(backtrace, (Frame) stack.elementAt(stack.size() - 1), null);
        }
        
        for (int i = traceSize; i > 0; i--) {
        	addBackTraceElement(backtrace, (Frame) stack.elementAt(i), (Frame) stack.elementAt(i-1));
        }

        return backtrace;
    }

	private static void addBackTraceElement(RubyArray backtrace, Frame frame, Frame previousFrame) {
        StringBuffer sb = new StringBuffer(100);
        SourcePosition position = frame.getPosition();

        sb.append(position.getFile()).append(':').append(position.getLine());

        if (previousFrame != null && previousFrame.getLastFunc() != null) {
            sb.append(":in `").append(previousFrame.getLastFunc()).append('\'');
        } else if (previousFrame == null && frame.getLastFunc() != null) {
            sb.append(":in `").append(frame.getLastFunc()).append('\'');
        }

        backtrace.append(backtrace.getRuntime().newString(sb.toString()));
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
        Ruby runtime = newException.getRuntime();

        if (runtime.getTraceFunction() != null) {
            runtime.callTraceFunction(
                "return",
                runtime.getPosition(),
                runtime.getCurrentFrame().getSelf(),
                runtime.getCurrentFrame().getLastFunc(),
                runtime.getCurrentFrame().getLastClass());
        }

        this.exception = newException;

        if (runtime.stackTraces > 5) {
            return;
        }

        runtime.stackTraces++;

        if (newException.callMethod("backtrace").isNil() && runtime.getSourceFile() != null) {
            IRubyObject backtrace = createBacktrace(runtime, 0, nativeException);
            newException.callMethod("set_backtrace", backtrace);
        }

        runtime.stackTraces--;
    }
}
