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
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.builtin.definitions.MathDefinition;

public class RubyMath {
    /** Create the Math module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createMathModule(Ruby ruby) {
        RubyModule mathModule = new MathDefinition(ruby).getModule();

        mathModule.defineConstant("E", RubyFloat.newFloat(ruby, Math.E));
        mathModule.defineConstant("PI", RubyFloat.newFloat(ruby, Math.PI));

        return mathModule;
    }

    public static RubyFloat atan2(IRubyObject recv, 
				  IRubyObject other, IRubyObject other2) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();
	double y = RubyNumeric.numericValue(other2).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), Math.atan2(x, y));
    }

    public static RubyFloat cos(IRubyObject recv, IRubyObject other) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), Math.cos(x));
    }

    public static RubyFloat exp(IRubyObject recv, IRubyObject other) {
	double exponent = RubyNumeric.numericValue(other).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), Math.exp(exponent));
    }

    /*
     * x = mantissa * 2 ** exponent
     *
     * Where mantissa is in the range of [.5, 1)
     *
     */
    public static RubyArray frexp(IRubyObject recv, IRubyObject other) {
	double mantissa = RubyNumeric.numericValue(other).getDoubleValue();
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
    public static RubyFloat ldexp(IRubyObject recv, IRubyObject x, 
				  IRubyObject y) {
	double mantissa = RubyNumeric.numericValue(x).getDoubleValue();
	double exponent = RubyNumeric.numericValue(y).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), 
				  mantissa * Math.pow(2.0, exponent));
    }

    /** Returns the natural logarithm of x.
     * 
     */
    public static RubyFloat log(IRubyObject recv, IRubyObject other) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), Math.log(x));
    }

    /** Returns the base 10 logarithm of x.
     * 
     */
    public static RubyFloat log10(IRubyObject recv, IRubyObject other) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), 
				  Math.log(x) / Math.log(10));
    }

    public static RubyFloat sin(IRubyObject recv, IRubyObject other) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), Math.sin(x));
    }

    public static RubyFloat sqrt(IRubyObject recv, IRubyObject other) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();

	if (x < 0) {
	    throw new ArgumentError(recv.getRuntime(), 
				    "square root for negative number");
	}
        return RubyFloat.newFloat(recv.getRuntime(), Math.sqrt(x));
    }

    public static RubyFloat tan(IRubyObject recv,  IRubyObject other) {
	double x = RubyNumeric.numericValue(other).getDoubleValue();

        return RubyFloat.newFloat(recv.getRuntime(), Math.tan(x));
    }
}
