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
 * @version 
 */
public class RubyException extends RubyObject {

    public RubyException(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }
    
    public static RubyClass createExceptionClass(Ruby ruby) {
        RubyClass exceptionClass = ruby.defineClass("Exception", ruby.getClasses().getObjectClass());
        
        exceptionClass.defineMethod("to_s", CallbackFactory.getMethod(RubyException.class, "to_s"));
        exceptionClass.defineMethod("to_str", CallbackFactory.getMethod(RubyException.class, "to_s"));
        exceptionClass.defineMethod("inspect", CallbackFactory.getMethod(RubyException.class, "inspect"));
        
        // exceptionClass.defineSingletonMethod("load_class", getSingletonMethod("m_load_class", RubyString.class, true));
        
        return exceptionClass;
    }


    public static RubyException newException(Ruby ruby, RubyClass excptnClass, String msg) {
        RubyException newException = new RubyException(ruby, excptnClass);
        newException.setInstanceVar("mesg", RubyString.newString(ruby, msg));
        return newException;
    }

    public static RubyException newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyException newException = new RubyException(ruby, (RubyClass)recv);
        if (args.length == 1) {
            newException.setInstanceVar("mesg", args[0]);
        }
        return newException;
    }

    public RubyException exception(RubyObject[] args) {
        switch (args.length) {
            case 0 :
                return this;
            case 1 :
                return (RubyException) newInstance(getRuby(), getRubyClass(), args);
            default :
                throw new RubyArgumentException(getRuby(), "Wrong argument count");
        }

    }

    public RubyString to_s() {
        RubyObject message = getInstanceVar("mesg");

        if (message.isNil()) {
            return getRubyClass().getClassPath();
        } else {
            message.setTaint(isTaint());

            return (RubyString) message;
        }
    }

    /** inspects an object and return a kind of debug information
     * 
     *@return A RubyString containing the debug information.
     */
    public RubyString inspect() {
        RubyModule rubyClass = getRubyClass();

        RubyString exception = RubyString.stringValue(this);

        if (exception.getValue().length() == 0) {
            return rubyClass.getClassPath();
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("#<");
            sb.append(rubyClass.getClassPath().getValue());
            sb.append(": ");
            sb.append(exception.getValue());
            sb.append(">");
            return RubyString.newString(getRuby(), sb.toString());
        }
    }
}