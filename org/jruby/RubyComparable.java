/*
 * RubyComparable.java - No description
 * Created on 11. September 2001, 22:51
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
public class RubyComparable {
    public static RubyModule createComparable(Ruby ruby) {
        RubyModule comparableModule = ruby.defineModule("Comparable");

        comparableModule.defineMethod(
            "==",
            CallbackFactory.getSingletonMethod(RubyComparable.class, "equal", RubyObject.class));
        comparableModule.defineMethod(">", CallbackFactory.getSingletonMethod(RubyComparable.class, "op_gt", RubyObject.class));
        comparableModule.defineMethod(
            ">=",
            CallbackFactory.getSingletonMethod(RubyComparable.class, "op_ge", RubyObject.class));
        comparableModule.defineMethod("<", CallbackFactory.getSingletonMethod(RubyComparable.class, "op_lt", RubyObject.class));
        comparableModule.defineMethod(
            "<=",
            CallbackFactory.getSingletonMethod(RubyComparable.class, "op_le", RubyObject.class));
        comparableModule.defineMethod(
            "between?",
            CallbackFactory.getSingletonMethod(RubyComparable.class, "between_p", RubyObject.class, RubyObject.class));

        return comparableModule;
    }

    public static RubyBoolean equal(Ruby ruby, RubyObject recv, RubyObject other) {
        if (recv == other) {
            return ruby.getTrue();
        } else {
            try {
                RubyFixnum fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), other);
                return RubyBoolean.newBoolean(ruby, fn.getValue() == 0);
            } catch (RubyNameException rnExcptn) {
                return ruby.getFalse();
            }
        }
    }

    public static RubyBoolean op_gt(Ruby ruby, RubyObject recv, RubyObject other) {
        RubyFixnum fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), other);
        return RubyBoolean.newBoolean(ruby, fn.getValue() > 0);
    }

    public static RubyBoolean op_ge(Ruby ruby, RubyObject recv, RubyObject other) {
        RubyFixnum fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), other);
        return RubyBoolean.newBoolean(ruby, fn.getValue() >= 0);
    }

    public static RubyBoolean op_lt(Ruby ruby, RubyObject recv, RubyObject other) {
        RubyFixnum fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), other);
        return RubyBoolean.newBoolean(ruby, fn.getValue() < 0);
    }

    public static RubyBoolean op_le(Ruby ruby, RubyObject recv, RubyObject other) {
        RubyFixnum fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), other);
        return RubyBoolean.newBoolean(ruby, fn.getValue() <= 0);
    }

    public static RubyBoolean between_p(Ruby ruby, RubyObject recv, RubyObject arg1, RubyObject arg2) {
        RubyFixnum fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), arg1);
        if (fn.getValue() < 0) {
            return ruby.getFalse();
        }

        fn = (RubyFixnum) recv.funcall(ruby.intern("<=>"), arg2);
        if (fn.getValue() > 0) {
            return ruby.getFalse();
        }

        return ruby.getTrue();
    }
}