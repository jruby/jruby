/*
 * RubyException.java - No description
 * Created on 18. Oktober 2001, 23:31
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2002 Anders Bengtsson
 * Copyright (C) 2002-2003 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se> 
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

package org.jruby;

import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyException extends RubyObject {

    private RubyArray backtrace;
    public IRubyObject message;

    private RubyException(Ruby runtime, RubyClass rubyClass) {
        this(runtime, rubyClass, null);
    }

    private RubyException(Ruby runtime, RubyClass rubyClass, String message) {
        super(runtime, rubyClass);
        if (message == null) {
            this.message = runtime.getNil();
        } else {
            this.message = RubyString.newString(runtime, message);
        }
    }

    public static RubyClass createExceptionClass(Ruby runtime) {
		RubyClass exceptionClass = runtime.defineClass("Exception", runtime.getClasses().getObjectClass());
    	
		CallbackFactory callbackFactory = runtime.callbackFactory();
        
		exceptionClass.defineSingletonMethod("new", 
				callbackFactory.getOptSingletonMethod(RubyException.class, "newInstance"));		
		exceptionClass.defineSingletonMethod("exception", 
				callbackFactory.getOptSingletonMethod(RubyException.class, "newInstance"));		
		exceptionClass.defineMethod("initialize",
			callbackFactory.getOptMethod(RubyException.class, "initialize"));
		exceptionClass.defineMethod("exception", 
			callbackFactory.getOptMethod(RubyException.class, "exception"));
		exceptionClass.defineMethod("to_s", 
			callbackFactory.getMethod(RubyException.class, "to_s"));
		exceptionClass.defineMethod("to_str", 
			callbackFactory.getMethod(RubyException.class, "to_s"));
		exceptionClass.defineMethod("message", 
			callbackFactory.getMethod(RubyException.class, "to_s"));
		exceptionClass.defineMethod("inspect", 
			callbackFactory.getMethod(RubyException.class, "inspect"));
		exceptionClass.defineMethod("backtrace", 
			callbackFactory.getMethod(RubyException.class, "backtrace"));		
		exceptionClass.defineMethod("set_backtrace", 
			callbackFactory.getMethod(RubyException.class, "set_backtrace", IRubyObject.class));		

		return exceptionClass;
    }

    public static RubyException newException(Ruby runtime, RubyClass excptnClass, String msg) {
        return new RubyException(runtime, excptnClass, msg);
    }

    // Exception methods

    public static RubyException newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyException newException = new RubyException(recv.getRuntime(), (RubyClass) recv);

        newException.callInit(args);

        return newException;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length > 0) {
            message = args[0];
        }
        return this;
    }

    public IRubyObject backtrace() {
        if (backtrace == null) {
            return runtime.getNil();
        }
        return backtrace;
    }

    public RubyArray set_backtrace(IRubyObject obj) {
        backtrace = RubyArray.arrayValue(obj);
        return backtrace;
    }

    public RubyException exception(IRubyObject[] args) {
        switch (args.length) {
            case 0 :
                return this;
            case 1 :
                if (args[0] == this) {
                    return this;
                }
                return newInstance(getMetaClass(), args);
            default :
                throw new ArgumentError(getRuntime(), "Wrong argument count");
        }

    }

    public RubyString to_s() {
        if (message.isNil()) {
            return RubyString.newString(getRuntime(), getMetaClass().getName());
        }
        message.setTaint(isTaint());
        return (RubyString) message.callMethod("to_s");
    }

    /** inspects an object and return a kind of debug information
     * 
     *@return A RubyString containing the debug information.
     */
    public RubyString inspect() {
        RubyModule rubyClass = getMetaClass();

        RubyString exception = RubyString.stringValue(this);

        if (exception.getValue().length() == 0) {
            return RubyString.newString(getRuntime(), rubyClass.getName());
        }
        StringBuffer sb = new StringBuffer();
        sb.append("#<");
        sb.append(rubyClass.getName());
        sb.append(": ");
        sb.append(exception.getValue());
        sb.append(">");
        return RubyString.newString(getRuntime(), sb.toString());
    }
}
