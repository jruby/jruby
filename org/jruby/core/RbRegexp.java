/*
 * RbRegexp.java - No description
 * Created on 21. Oktober 2001, 16:57
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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

package org.jruby.core;

import org.jruby.*;
import org.jruby.parser.ReOptions;

/**
 *
 * @author  amoore
 */
public class RbRegexp implements ReOptions {
    public static RubyClass createRegexpClass(Ruby ruby) {
        RubyClass regexpClass = ruby.defineClass("Regexp", ruby.getClasses().getObjectClass());

        regexpClass.defineConstant("IGNORECASE", RubyFixnum.newFixnum(ruby, RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", RubyFixnum.newFixnum(ruby, RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", RubyFixnum.newFixnum(ruby, RE_OPTION_MULTILINE));
        
        regexpClass.defineSingletonMethod("new", getSingletonMethod("m_new", true));
        regexpClass.defineSingletonMethod("compile", getSingletonMethod("m_new", true));
        regexpClass.defineSingletonMethod("quote", getSingletonMethod("m_quote", RubyString.class));
        regexpClass.defineSingletonMethod("escape", getSingletonMethod("m_quote", RubyString.class));
        regexpClass.defineSingletonMethod("last_match", getSingletonMethod("m_last_match", false));

        regexpClass.defineMethod("initialize", getRestArgsMethod("m_initialize"));
        regexpClass.defineMethod("clone", getMethod("m_clone"));
        regexpClass.defineMethod("==", getMethod("m_equal", RubyObject.class));
        regexpClass.defineMethod("===", getMethod("m_match", RubyObject.class));
        regexpClass.defineMethod("=~", getMethod("m_match", RubyObject.class));
        regexpClass.defineMethod("~", getMethod("m_match2"));
        regexpClass.defineMethod("match", getMethod("m_match_m", RubyObject.class));
        regexpClass.defineMethod("inspect", getMethod("m_inspect"));
        regexpClass.defineMethod("source", getMethod("m_source"));
        regexpClass.defineMethod("casefold?", getMethod("m_casefold"));
//        regexpClass.defineMethod("kcode", getMethod("m_kcode"));
        
        return regexpClass;
    }
    
    public static Callback getSingletonMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyRegexp.class, methodName, RubyObject[].class, true, true);
        } else {
            return new ReflectionCallbackMethod(RubyRegexp.class, methodName, false, true);
        }
    }
    
    public static Callback getSingletonMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyRegexp.class, methodName, arg1, false, true);
    }
    
    public static Callback getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyRegexp.class, methodName);
    }
    
    public static Callback getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyRegexp.class, methodName, arg1);
    }
    
    public static Callback getRestArgsMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyRegexp.class, methodName, RubyObject[].class, true);
    }
}
