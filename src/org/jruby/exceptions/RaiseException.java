/*
 * RaiseException.java - No description
 * Created on 18.01.2002, 22:26:09
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyString;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.Frame;
import org.jruby.runtime.FrameStack;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

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
        RubyArray backtrace = RubyArray.newArray(runtime);
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

        backtrace.append(RubyString.newString(backtrace.getRuntime(), sb.toString()));
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
