/*
 * RubyException.java - No description
 * Created on 18. Oktober 2001, 23:31
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyException extends RubyObject {

    private RubyArray backtrace;
    public RubyObject message;

    private RubyException(Ruby ruby, RubyClass rubyClass) {
        this(ruby, rubyClass, null);
    }

    private RubyException(Ruby ruby, RubyClass rubyClass, String message) {
        super(ruby, rubyClass);
        if (message == null) {
            this.message = ruby.getNil();
        } else {
            this.message = RubyString.newString(ruby, message);
        }
    }

    public static RubyClass createExceptionClass(Ruby ruby) {
        RubyClass exceptionClass = ruby.defineClass("Exception", ruby.getClasses().getObjectClass());

        exceptionClass.defineSingletonMethod(
            "exception",
            CallbackFactory.getOptSingletonMethod(RubyException.class, "newInstance"));

        exceptionClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyException.class, "initialize"));
        exceptionClass.defineMethod("exception", CallbackFactory.getOptMethod(RubyException.class, "exception"));

        exceptionClass.defineMethod("to_s", CallbackFactory.getMethod(RubyException.class, "to_s"));
        exceptionClass.defineMethod("to_str", CallbackFactory.getMethod(RubyException.class, "to_s"));
        exceptionClass.defineMethod("message", CallbackFactory.getMethod(RubyException.class, "to_s"));
        exceptionClass.defineMethod("inspect", CallbackFactory.getMethod(RubyException.class, "inspect"));

        exceptionClass.defineMethod("backtrace", CallbackFactory.getMethod(RubyException.class, "backtrace"));
        exceptionClass.defineMethod(
            "set_backtrace",
            CallbackFactory.getMethod(RubyException.class, "set_backtrace", RubyArray.class));

        return exceptionClass;
    }

    public static RubyException newException(Ruby ruby, RubyClass excptnClass, String msg) {
        return new RubyException(ruby, excptnClass, msg);
    }

    // Exception methods

    public static RubyException newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyException newException = new RubyException(ruby, (RubyClass) recv);

        newException.callInit(args);

        return newException;
    }

    public RubyObject initialize(RubyObject[] args) {
        if (args.length > 0) {
            message = args[0];
        }
        return this;
    }

    public RubyArray backtrace() {
        if (backtrace == null) {
            return RubyArray.nilArray(ruby);
        }
        return backtrace;
    }

    public RubyArray set_backtrace(RubyArray newBacktrace) {
        backtrace = newBacktrace;
        return newBacktrace;
    }

    public RubyException exception(RubyObject[] args) {
        switch (args.length) {
            case 0 :
                return this;
            case 1 :
            	if (args[0] == this) {
            	    return this;
            	} else {
                	return (RubyException) newInstance(getRuby(), getInternalClass(), args);
            	}
            default :
                throw new ArgumentError(getRuby(), "Wrong argument count");
        }

    }

    public RubyString to_s() {
        if (message.isNil()) {
            return RubyString.newString(getRuby(), getInternalClass().getClassPath());
        } else {
            message.setTaint(isTaint());
            return (RubyString) message.callMethod("to_s");
        }
    }

    /** inspects an object and return a kind of debug information
     * 
     *@return A RubyString containing the debug information.
     */
    public RubyString inspect() {
        RubyModule rubyClass = getInternalClass();

        RubyString exception = RubyString.stringValue(this);

        if (exception.getValue().length() == 0) {
            return RubyString.newString(getRuby(), rubyClass.getClassPath());
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("#<");
            sb.append(rubyClass.getClassPath());
            sb.append(": ");
            sb.append(exception.getValue());
            sb.append(">");
            return RubyString.newString(getRuby(), sb.toString());
        }
    }
}
