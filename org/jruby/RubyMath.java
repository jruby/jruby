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

import org.jruby.runtime.*;

public class RubyMath {
    /** Create the Math module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createMathModule(Ruby ruby) {
        Callback atan2 =
            new ReflectionCallbackMethod(RubyMath.class, "atan2", new Class[] { RubyNumeric.class, RubyNumeric.class });
        Callback cos = new ReflectionCallbackMethod(RubyMath.class, "cos", RubyNumeric.class);
        Callback exp = new ReflectionCallbackMethod(RubyMath.class, "exp", RubyNumeric.class);
        Callback frexp = new ReflectionCallbackMethod(RubyMath.class, "frexp", RubyNumeric.class);
        Callback ldexp =
            new ReflectionCallbackMethod(RubyMath.class, "ldexp", new Class[] { RubyFloat.class, RubyInteger.class });
        Callback log = new ReflectionCallbackMethod(RubyMath.class, "log", RubyNumeric.class);
        Callback log10 = new ReflectionCallbackMethod(RubyMath.class, "log10", RubyNumeric.class);
        Callback sin = new ReflectionCallbackMethod(RubyMath.class, "sin", RubyNumeric.class);
        Callback sqrt = new ReflectionCallbackMethod(RubyMath.class, "sqrt", RubyNumeric.class);
        Callback tan = new ReflectionCallbackMethod(RubyMath.class, "tan", RubyNumeric.class);

        RubyModule mathModule = ruby.defineModule("Math");

        mathModule.defineConstant("E", RubyFloat.newFloat(ruby, Math.E));
        mathModule.defineConstant("PI", RubyFloat.newFloat(ruby, Math.PI));

        mathModule.defineSingletonMethod("atan2", atan2);
        mathModule.defineSingletonMethod("cos", cos);
        mathModule.defineSingletonMethod("exp", exp);
//        mathModule.defineSingletonMethod("frexp", frexp);
        mathModule.defineSingletonMethod("ldexp", ldexp);
        mathModule.defineSingletonMethod("log", log);
        mathModule.defineSingletonMethod("log10", log10);
        mathModule.defineSingletonMethod("sin", sin);
        mathModule.defineSingletonMethod("sqrt", sqrt);
        mathModule.defineSingletonMethod("tan", tan);

        return mathModule;
    }

    public static RubyFloat atan2(Ruby ruby, RubyObject recv, RubyNumeric x, RubyNumeric y) {
        return RubyFloat.newFloat(ruby, Math.atan2(x.getDoubleValue(), y.getDoubleValue()));
    }

    public static RubyFloat cos(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.cos(x.getDoubleValue()));
    }

    public static RubyFloat exp(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.exp(x.getDoubleValue()));
    }

    public static RubyArray frexp(Ruby ruby, RubyObject recv, RubyNumeric x) {
        // return RubyFloat.m_newFloat(ruby, Math.exp(x.getDoubleValue()));
        return null;
    }

    public static RubyFloat ldexp(Ruby ruby, RubyObject recv, RubyFloat x, RubyInteger y) {
        return RubyFloat.newFloat(ruby, x.getDoubleValue() * Math.pow(2.0, y.getDoubleValue()));
    }

    /** Returns the natural logarithm of x.
     * 
     */
    public static RubyFloat log(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.log(x.getDoubleValue()));
    }

    /** Returns the base 10 logarithm of x.
     * 
     */
    public static RubyFloat log10(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.log(x.getDoubleValue()) / Math.log(10));
    }

    public static RubyFloat sin(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.sin(x.getDoubleValue()));
    }

    public static RubyFloat sqrt(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.sqrt(x.getDoubleValue()));
    }

    public static RubyFloat tan(Ruby ruby, RubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(ruby, Math.tan(x.getDoubleValue()));
    }
}
