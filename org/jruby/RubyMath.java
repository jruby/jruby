/*
 * RubyMath.java - No description
 * Created on 25. November 2001, 17:36
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyMath {
    /** Create the Math module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createMathModule(Ruby ruby) {
        RubyModule mathModule = ruby.defineModule("Math");

        mathModule.defineConstant("E", RubyFloat.newFloat(ruby, Math.E));
        mathModule.defineConstant("PI", RubyFloat.newFloat(ruby, Math.PI));

        mathModule.defineSingletonMethod(
            "atan2",
            CallbackFactory.getSingletonMethod(RubyMath.class, "atan2", RubyNumeric.class, RubyNumeric.class));
        mathModule.defineSingletonMethod("cos", CallbackFactory.getSingletonMethod(RubyMath.class, "cos", RubyNumeric.class));
        mathModule.defineSingletonMethod("exp", CallbackFactory.getSingletonMethod(RubyMath.class, "exp", RubyNumeric.class));
        //        mathModule.defineSingletonMethod("frexp", frexp);
        mathModule.defineSingletonMethod("ldexp", CallbackFactory.getSingletonMethod(RubyMath.class, "ldexp", RubyFloat.class, RubyInteger.class));
        mathModule.defineSingletonMethod("log", CallbackFactory.getSingletonMethod(RubyMath.class, "log", RubyNumeric.class));
        mathModule.defineSingletonMethod("log10", CallbackFactory.getSingletonMethod(RubyMath.class, "log10", RubyNumeric.class));
        mathModule.defineSingletonMethod("sin", CallbackFactory.getSingletonMethod(RubyMath.class, "sin", RubyNumeric.class));
        mathModule.defineSingletonMethod("sqrt", CallbackFactory.getSingletonMethod(RubyMath.class, "sqrt", RubyNumeric.class));
        mathModule.defineSingletonMethod("tan", CallbackFactory.getSingletonMethod(RubyMath.class, "tan", RubyNumeric.class));

        return mathModule;
    }

    public static RubyFloat atan2(IRubyObject recv, RubyNumeric x, RubyNumeric y) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.atan2(x.getDoubleValue(), y.getDoubleValue()));
    }

    public static RubyFloat cos(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.cos(x.getDoubleValue()));
    }

    public static RubyFloat exp(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.exp(x.getDoubleValue()));
    }

    public static RubyArray frexp(IRubyObject recv, RubyNumeric x) {
        // return RubyFloat.m_newFloat(ruby, Math.exp(x.getDoubleValue()));
        return null;
    }

    public static RubyFloat ldexp(IRubyObject recv, RubyFloat x, RubyInteger y) {
        return RubyFloat.newFloat(recv.getRuntime(), x.getDoubleValue() * Math.pow(2.0, y.getDoubleValue()));
    }

    /** Returns the natural logarithm of x.
     * 
     */
    public static RubyFloat log(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.log(x.getDoubleValue()));
    }

    /** Returns the base 10 logarithm of x.
     * 
     */
    public static RubyFloat log10(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.log(x.getDoubleValue()) / Math.log(10));
    }

    public static RubyFloat sin(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.sin(x.getDoubleValue()));
    }

    public static RubyFloat sqrt(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.sqrt(x.getDoubleValue()));
    }

    public static RubyFloat tan(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), Math.tan(x.getDoubleValue()));
    }
}
