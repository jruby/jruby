/*
 * RbEnumerable.java - No description
 * Created on 25. September 2001, 17:05
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

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RbEnumerable {

    public static RubyModule createEnumerableModule(Ruby ruby) {
        RubyModule enumerableModule = ruby.defineModule("Enumerable");
        
        enumerableModule.defineMethod("entries", getSingletonMethod("m_to_a"));
        enumerableModule.defineMethod("to_a", getSingletonMethod("m_to_a"));
        enumerableModule.defineMethod("sort", getSingletonMethod("m_sort"));
        
        return enumerableModule;
    }

    public static RubyObject each(Ruby ruby, RubyObject recv) {
        return recv.funcall(ruby.intern("each"));
    }
    
    public static RubyObject enum_all(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        ((RubyArray)arg1).m_push(blockArg);
        
        return ruby.getNil();
    }
    
    /*public static RubyObject grep_iter(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        if (RubyArray)arg1))
        
        ((RubyArray)arg1).m_push(blockArg);
        
        return ruby.getNil();
    }

    public static RubyObject grep(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        ((RubyArray)arg1).m_push(blockArg);
        
        return ruby.getNil();
    }*/
    
    /* methods of the Enumerable module. */
    
    public static RubyObject m_to_a(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.m_newArray(ruby);
        
        ruby.iterate(getSingletonMethod("each"), recv, getBlockMethod("enum_all"), ary);
        
        return ary;
    }
    
    public static RubyObject m_sort(Ruby ruby, RubyObject recv) {
        RubyArray ary = (RubyArray)m_to_a(ruby, recv);
        
        ary.m_sort_bang();
        
        return ary;
    }
    
    public static ReflectionCallbackMethod getSingletonMethod(String methodName) {
        return new ReflectionCallbackMethod(RbEnumerable.class, methodName, false, true);
    }
    
    public static ReflectionCallbackMethod getSingletonMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RbEnumerable.class, methodName, arg1, false, true);
    }
    
    public static ReflectionCallbackMethod getBlockMethod(String methodName) {
        return new ReflectionCallbackMethod(RbEnumerable.class, methodName, new Class[] { RubyObject.class, RubyObject.class }, false, true);
    }
}