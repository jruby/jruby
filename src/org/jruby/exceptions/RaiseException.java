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

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RaiseException extends JumpException {
    private RubyException exception;

    public RaiseException(RubyException actException) {
        setException(actException);
    }

    public RaiseException(Ruby ruby, RubyClass excptnClass, String msg) {
		super(msg);
        setException(RubyException.newException(ruby, excptnClass, msg));
    }

    public RaiseException(Ruby ruby, String excptnClassName, String msg) {
		super(msg);
        RubyClass excptnClass = ruby.getClass(excptnClassName);
        if (excptnClass == null) {
            System.err.println(excptnClassName);
        }
        setException(RubyException.newException(ruby, excptnClass, msg));
    }

    public Throwable fillInStackTrace() {
        return originalFillInStackTrace();
    }

    /** 
     * Create an Array with backtrace information.
     */
    public static RubyArray createBacktrace(Ruby ruby, int level) {
        RubyArray backtrace = RubyArray.newArray(ruby);
        FrameStack stack = ruby.getFrameStack();
        int traceSize = stack.size() - level - 1;
        
        if (traceSize <= 0) {
        	return RubyArray.nilArray(ruby);
        }
        
        for (int i = traceSize; i > 0; i--) {
        	addBackTraceElement(backtrace, (Frame) stack.elementAt(i), 
        			(Frame) stack.elementAt(i-1));
        }

        return backtrace;
    }

	private static void addBackTraceElement(RubyArray backtrace, Frame frame, 
			Frame previousFrame) {
        StringBuffer sb = new StringBuffer(100);
        SourcePosition position = frame.getPosition();

        sb.append(position.getFile()).append(':').append(position.getLine());
        
        if (previousFrame != null && previousFrame.getLastFunc() != null) {
            sb.append(":in `").append(previousFrame.getLastFunc()).append('\'');
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
    protected void setException(RubyException newException) {
        Ruby ruby = newException.getRuntime();

        if (ruby.getTraceFunction() != null) {
            ruby.callTraceFunction(
                "return",
                ruby.getPosition(),
                ruby.getCurrentFrame().getSelf(),
                ruby.getCurrentFrame().getLastFunc(),
                ruby.getCurrentFrame().getLastClass());
        }

        this.exception = newException;

        if (ruby.stackTraces > 5) {
            return;
        }

        ruby.stackTraces++;

        if (newException.callMethod("backtrace").isNil() && ruby.getSourceFile() != null) {
            newException.callMethod("set_backtrace", createBacktrace(ruby, 1));
        }

        ruby.stackTraces--;
    }
}
