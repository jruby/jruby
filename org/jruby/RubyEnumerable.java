/*
 * RubyEnumerable.java - No description
 * Created on 25. September 2001, 17:05
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

import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyEnumerable {

    public static RubyModule createEnumerableModule(Ruby ruby) {
        RubyModule enumerableModule = ruby.defineModule("Enumerable");

        enumerableModule.defineMethod("entries", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "to_a"));
        enumerableModule.defineMethod("to_a", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "to_a"));
        enumerableModule.defineMethod("sort", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "sort"));

        return enumerableModule;
    }

    public static RubyObject each(Ruby ruby, RubyObject recv) {
        return recv.funcall("each");
    }

    public static RubyObject enum_all(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        ((RubyArray) arg1).push(blockArg);

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

    public static RubyObject to_a(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);

        ruby.iterate(
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "each"),
            recv,
            CallbackFactory.getBlockMethod(RubyEnumerable.class, "enum_all"),
            ary);

        return ary;
    }

    public static RubyObject sort(Ruby ruby, RubyObject recv) {
        RubyArray ary = (RubyArray) to_a(ruby, recv);

        ary.sort_bang();

        return ary;
    }

}
