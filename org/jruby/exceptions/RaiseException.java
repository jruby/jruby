/*
 * RaiseException.java - No description
 * Created on 18.01.2002, 22:26:09
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import java.util.*;

import org.jruby.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RaiseException extends JumpException {
    private RubyException actException;

    public RaiseException(RubyException actException) {
        setActException(actException);
    }

    public RaiseException(Ruby ruby, RubyClass excptnClass, String msg) {
		super(msg);
        setActException(RubyException.newException(ruby, excptnClass, msg));
    }

    public RaiseException(Ruby ruby, String excptnClassName, String msg) {
		super(msg);
        RubyClass excptnClass = (RubyClass) ruby.getRubyModule(excptnClassName);
        if (excptnClass == null) {
            System.err.println(excptnClassName);
        }
        setActException(RubyException.newException(ruby, excptnClass, msg));
    }

    public Throwable fillInStackTrace() {
        return originalFillInStackTrace();
    }

    /** Create an Array with backtrace information.
     * 
     * MRI: eval.c - backtrace
     * 
     */
    public static RubyArray createBacktrace(Ruby ruby, int level) {
        RubyArray backtrace = RubyArray.newArray(ruby);

        /* Benoit: this generates the first line of the backtrace once too many time, see test/testException
        if (level < 0) {
            StringBuffer sb = new StringBuffer(100);
        
            if (frame.getLastFunc() != null) {
                sb.append(ruby.getSourceFile()).append(':').append(ruby.getSourceLine());
                sb.append(":in '").append(frame.getLastFunc()).append('\'');
            } else if (ruby.getSourceLine() == 0) {
                sb.append(ruby.getSourceFile());
            } else {
                sb.append(ruby.getSourceFile()).append(':').append(ruby.getSourceLine());
            }
            backtrace.append(RubyString.newString(ruby, sb.toString()));
        } else {*/

        Iterator iter = ruby.getFrameStack().iterator();

        while (level-- > 0) {
            if (!iter.hasNext()) {
                return RubyArray.nilArray(ruby);
            }
            iter.next();
        }
        // }

        Frame frame = null;
        if (iter.hasNext()) {
            frame = (Frame)iter.next();
        }

        while (frame != null && frame.getFile() != null) {
            StringBuffer sb = new StringBuffer(100);

            Frame previous = null;
            if (iter.hasNext() && (previous = (Frame)iter.next()).getLastFunc() != null) {
                sb.append(frame.getFile()).append(':').append(frame.getLine());
                sb.append(":in '").append(previous.getLastFunc()).append('\'');
            } else {
                sb.append(frame.getFile()).append(':').append(frame.getLine());
            }
            backtrace.append(RubyString.newString(ruby, sb.toString()));

            frame = previous;
        }

        return backtrace;
    }

    /**
     * Gets the actException
     * @return Returns a RubyException
     */
    public RubyException getActException() {
        return actException;
    }

    /**
     * Sets the actException
     * @param actException The actException to set
     */
    protected void setActException(RubyException actException) {
        Ruby ruby = actException.getRuby();

        // XXX Maybe move it into another methods.
        if (ruby.getRuntime().getTraceFunction() != null) {
            ruby.getRuntime().callTraceFunction(
                "return",
                ruby.getSourceFile(),
                ruby.getSourceLine(),
                ruby.getCurrentFrame().getSelf(),
                ruby.getCurrentFrame().getLastFunc(),
                ruby.getCurrentFrame().getLastClass());
        }

        this.actException = actException;

        if (ruby.stackTraces > 5) {
            return;
        }

        ruby.stackTraces++;

        if (actException.funcall("backtrace").isNil() && ruby.getSourceFile() != null) {
            actException.funcall("set_backtrace", createBacktrace(ruby, -1));
        }

        ruby.stackTraces--;
    }
}
