/*
 * RbMatchDataClass.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

/**
 *
 * @author  amoore
 */
public class RbMatchData {
    public static RubyClass createMatchDataClass(Ruby ruby) {
        RubyClass matchDataClass = ruby.defineClass("MatchData", ruby.getClasses().getObjectClass());
        ruby.defineGlobalConstant("MatchingData", matchDataClass);
        
        matchDataClass.defineMethod("clone", getMethod("m_clone"));
        matchDataClass.defineMethod("size", getMethod("m_size"));
        matchDataClass.defineMethod("length", getMethod("m_size"));
        matchDataClass.defineMethod("offset", getMethod("m_offset", RubyFixnum.class));
        matchDataClass.defineMethod("begin", getMethod("m_begin", RubyFixnum.class));
        matchDataClass.defineMethod("end", getMethod("m_end", RubyFixnum.class));
        matchDataClass.defineMethod("to_a", getMethod("m_to_a"));
        matchDataClass.defineMethod("[]", getRestArgsMethod("m_aref"));
        matchDataClass.defineMethod("pre_match", getMethod("m_pre_match"));
        matchDataClass.defineMethod("post_match", getMethod("m_post_match"));
        matchDataClass.defineMethod("to_s", getMethod("m_to_s"));
        matchDataClass.defineMethod("string", getMethod("m_string"));
        
        matchDataClass.getRubyClass().undefMethod("new");
        
        return matchDataClass;
    }
    
    public static RubyCallbackMethod getRestArgsMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyMatchData.class, methodName, RubyObject[].class, true);
    }
    
    public static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyMatchData.class, methodName);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyMatchData.class, methodName, arg1);
    }
}
