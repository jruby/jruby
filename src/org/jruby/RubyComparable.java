/*
 * RubyComparable.java - Implementation of the Comparable module.
 * Created on 11. September 2001, 22:51
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.exceptions.NameError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/** Implementation of the Comparable module.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyComparable {
    public static RubyModule createComparable(Ruby ruby) {
        RubyModule comparableModule = ruby.defineModule("Comparable");
        CallbackFactory callbackFactory = ruby.callbackFactory();
        comparableModule.defineMethod(
            "==",
            callbackFactory.getSingletonMethod(RubyComparable.class, "equal", IRubyObject.class));
        comparableModule.defineMethod(
            ">",
            callbackFactory.getSingletonMethod(RubyComparable.class, "op_gt", IRubyObject.class));
        comparableModule.defineMethod(
            ">=",
            callbackFactory.getSingletonMethod(RubyComparable.class, "op_ge", IRubyObject.class));
        comparableModule.defineMethod(
            "<",
            callbackFactory.getSingletonMethod(RubyComparable.class, "op_lt", IRubyObject.class));
        comparableModule.defineMethod(
            "<=",
            callbackFactory.getSingletonMethod(RubyComparable.class, "op_le", IRubyObject.class));
        comparableModule.defineMethod(
            "between?",
            callbackFactory.getSingletonMethod(
                RubyComparable.class,
                "between_p",
                IRubyObject.class,
                IRubyObject.class));

        return comparableModule;
    }

    public static RubyBoolean equal(IRubyObject recv, IRubyObject other) {
        try {
            if (recv == other) {
                return recv.getRuntime().getTrue();
            } else {
                return (RubyNumeric.fix2int(recv.callMethod("<=>", other)) == 0) ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
            }
        } catch (NameError rnExcptn) {
            return recv.getRuntime().getFalse();
        }
    }

    public static RubyBoolean op_gt(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.fix2int(recv.callMethod("<=>", other)) > 0 ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static RubyBoolean op_ge(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.fix2int(recv.callMethod("<=>", other)) >= 0 ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static RubyBoolean op_lt(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.fix2int(recv.callMethod("<=>", other)) < 0 ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static RubyBoolean op_le(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.fix2int(recv.callMethod("<=>", other)) <= 0 ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static RubyBoolean between_p(IRubyObject recv, IRubyObject first, IRubyObject second) {
        if (RubyNumeric.fix2int(recv.callMethod("<=>", first)) < 0) {
            return recv.getRuntime().getFalse();
        } else if (RubyNumeric.fix2int(recv.callMethod("<=>", second)) > 0) {
            return recv.getRuntime().getFalse();
        } else {
            return recv.getRuntime().getTrue();
        }
    }
}