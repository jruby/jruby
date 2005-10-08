/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyMath {
    /** Create the Math module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createMathModule(IRuby runtime) {
        RubyModule result = runtime.defineModule("Math");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyMath.class);
        
        result.defineConstant("E", RubyFloat.newFloat(runtime, Math.E));
        result.defineConstant("PI", RubyFloat.newFloat(runtime, Math.PI));

        result.defineModuleFunction("atan2", callbackFactory.getSingletonMethod("atan2", RubyNumeric.class, RubyNumeric.class));
        result.defineModuleFunction("cos", callbackFactory.getSingletonMethod("cos", RubyNumeric.class));
        result.defineModuleFunction("exp", callbackFactory.getSingletonMethod("exp", RubyNumeric.class));
        result.defineModuleFunction("frexp", callbackFactory.getSingletonMethod("frexp", RubyNumeric.class));
        result.defineModuleFunction("ldexp", callbackFactory.getSingletonMethod("ldexp", RubyNumeric.class, RubyNumeric.class));
        result.defineModuleFunction("log", callbackFactory.getSingletonMethod("log", RubyNumeric.class));
        result.defineModuleFunction("log10", callbackFactory.getSingletonMethod("log10", RubyNumeric.class));
        result.defineModuleFunction("sin", callbackFactory.getSingletonMethod("sin", RubyNumeric.class));
        result.defineModuleFunction("sqrt", callbackFactory.getSingletonMethod("sqrt", RubyNumeric.class));
        result.defineModuleFunction("tan", callbackFactory.getSingletonMethod("tan", RubyNumeric.class));
        
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
	 
        RubyArray result = recv.getRuntime().newArray(2);
        
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
        	return RubyFloat.newFloat(recv.getRuntime(), Double.NaN);
        }
        
        return RubyFloat.newFloat(recv.getRuntime(), Math.sqrt(x));
    }

    public static RubyFloat tan(IRubyObject recv,  RubyNumeric x) {
        return RubyFloat.newFloat(recv.getRuntime(), 
                Math.tan(x.getDoubleValue()));
    }
}
