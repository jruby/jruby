/*
 * RubyMath.java - No description
 * Created on 25. November 2001, 17:36
 * 
 * Copyright (C) 2001,2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore,
 *    Benoit Cerrina
 * Copyright (C) 2002 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

public class RubyMath {
    /** Create the Math module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createMathModule(Ruby runtime) {
        RubyModule result = runtime.defineModule("Math");
        CallbackFactory callbackFactory = runtime.callbackFactory();
        
        result.defineConstant("E", RubyFloat.newFloat(runtime, Math.E));
        result.defineConstant("PI", RubyFloat.newFloat(runtime, Math.PI));

        result.defineSingletonMethod("atan2", callbackFactory.getSingletonMethod(RubyMath.class, "atan2", RubyNumeric.class, RubyNumeric.class));
        result.defineSingletonMethod("cos", callbackFactory.getSingletonMethod(RubyMath.class, "cos", RubyNumeric.class));
        result.defineSingletonMethod("exp", callbackFactory.getSingletonMethod(RubyMath.class, "exp", RubyNumeric.class));
        result.defineSingletonMethod("frexp", callbackFactory.getSingletonMethod(RubyMath.class, "frexp", RubyNumeric.class));
        result.defineSingletonMethod("ldexp", callbackFactory.getSingletonMethod(RubyMath.class, "ldexp", RubyNumeric.class, RubyNumeric.class));
        result.defineSingletonMethod("log", callbackFactory.getSingletonMethod(RubyMath.class, "log", RubyNumeric.class));
        result.defineSingletonMethod("log10", callbackFactory.getSingletonMethod(RubyMath.class, "log10", RubyNumeric.class));
        result.defineSingletonMethod("sin", callbackFactory.getSingletonMethod(RubyMath.class, "sin", RubyNumeric.class));
        result.defineSingletonMethod("sqrt", callbackFactory.getSingletonMethod(RubyMath.class, "sqrt", RubyNumeric.class));
        result.defineSingletonMethod("tan", callbackFactory.getSingletonMethod(RubyMath.class, "tan", RubyNumeric.class));
        
        return result;
    }

    public static RubyFloat atan2(IRubyObject recv, RubyNumeric x, 
            RubyNumeric y) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.atan2(x.getDoubleValue(), y.getDoubleValue()));
    }

    public static RubyFloat cos(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.cos(x.getDoubleValue()));
    }

    public static RubyFloat exp(IRubyObject recv, RubyNumeric exponent) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.exp(exponent.getDoubleValue()));
    }

    /*
     * x = mantissa * 2 ** exponent
     *
     * Where mantissa is in the range of [.5, 1)
     *
     */
    public static RubyArray frexp(IRubyObject recv, RubyNumeric other) {
        double mantissa = other.getDoubleValue();
        short sign = 1;
        double exponent = 0;

        if (mantissa != 0.0) {
            // Make mantissa same sign so we only have one code path.
            if (mantissa < 0) {
                mantissa = -mantissa;
                sign = -1;
            }

            // Increase value to hit lower range.
            for (; mantissa < 0.5; mantissa *= 2.0, exponent -=1) { }

            // Decrease value to hit upper range.  
            for (; mantissa >= 1.0; mantissa *= 0.5, exponent +=1) { }
        }
	 
        RubyArray result = RubyArray.newArray(recv.getRuntime(), 2);
        
        result.append(RubyFloat.newFloat(recv.getRuntime(), sign * mantissa));
        result.append(RubyFloat.newFloat(recv.getRuntime(), exponent));
	
        return result;
    }

    /*
     * r = x * 2 ** y
     */
    public static RubyFloat ldexp(IRubyObject recv, RubyNumeric mantissa, 
				  RubyNumeric exponent) {
        return RubyFloat.newFloat(recv.getRuntime(), 
				  mantissa.getDoubleValue() * 
				  Math.pow(2.0, exponent.getDoubleValue()));
    }

    /** Returns the natural logarithm of x.
     * 
     */
    public static RubyFloat log(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.log(x.getDoubleValue()));
    }

    /** Returns the base 10 logarithm of x.
     * 
     */
    public static RubyFloat log10(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), 
				  Math.log(x.getDoubleValue()) / Math.log(10));
    }

    public static RubyFloat sin(IRubyObject recv, RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.sin(x.getDoubleValue()));
    }

    public static RubyFloat sqrt(IRubyObject recv, RubyNumeric other) {
        double x = other.getDoubleValue();

        if (x < 0) {
            throw new ArgumentError(recv.getRuntime(), 
            	"square root for negative number");
        }
        
        return RubyFloat.newFloat(recv.getRuntime(), Math.sqrt(x));
    }

    public static RubyFloat tan(IRubyObject recv,  RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.tan(x.getDoubleValue()));
    }
}
