/*
 * RubyInteger.java - Implementation of the Integer class.
 * Created on 10. September 2001, 17:49
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
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

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RangeError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class RubyInteger extends RubyNumeric {

    public RubyInteger(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public static RubyClass createIntegerClass(Ruby ruby) {
        RubyClass integerClass = ruby.defineClass("Integer", ruby.getClasses().getNumericClass());

        integerClass.getMetaClass().undefMethod("new");

        integerClass.defineSingletonMethod("induced_from", CallbackFactory.getSingletonMethod(RubyInteger.class, "induced_from", IRubyObject.class));

        integerClass.defineMethod("chr", CallbackFactory.getMethod(RubyInteger.class, "chr"));
        integerClass.defineMethod("integer?", CallbackFactory.getMethod(RubyInteger.class, "int_p"));
        integerClass.defineMethod("to_i", CallbackFactory.getSelfMethod(0));
        integerClass.defineMethod("to_int", CallbackFactory.getSelfMethod(0));
        integerClass.defineMethod("ceil", CallbackFactory.getSelfMethod(0));
        integerClass.defineMethod("floor", CallbackFactory.getSelfMethod(0));
        integerClass.defineMethod("round", CallbackFactory.getSelfMethod(0));
        integerClass.defineMethod("truncate", CallbackFactory.getSelfMethod(0));

        integerClass.defineMethod("next", CallbackFactory.getMethod(RubyInteger.class, "succ"));
        integerClass.defineMethod("succ", CallbackFactory.getMethod(RubyInteger.class, "succ"));

        integerClass.defineMethod("downto", CallbackFactory.getMethod(RubyInteger.class, "downto", RubyNumeric.class));
        integerClass.defineMethod("step", CallbackFactory.getMethod(RubyInteger.class, "step", RubyNumeric.class, RubyNumeric.class));
        integerClass.defineMethod("times", CallbackFactory.getMethod(RubyInteger.class, "times"));
        integerClass.defineMethod("upto", CallbackFactory.getMethod(RubyInteger.class, "upto", RubyNumeric.class));

        return integerClass;
    }
    
    // conversion
    protected RubyFloat toFloat() {
        return RubyFloat.newFloat(runtime, getDoubleValue());
    }
    
    
    // Integer methods

    public static RubyInteger induced_from(IRubyObject recv, IRubyObject number) {
        if (number instanceof RubyNumeric) {
            return (RubyInteger) number.callMethod("to_i");
        } else {
            throw new TypeError(recv.getRuntime(), "failed to convert " + number.getMetaClass() + " into Integer");
        }
    }

    public RubyString chr() {
        if (getLongValue() < 0 || getLongValue() > 0xff) {
            throw new RangeError(getRuntime(), this.toString() + " out of char range");
        }
        return RubyString.newString(getRuntime(), new String(new char[] {(char) getLongValue()}));
    }

    public IRubyObject downto(RubyNumeric to) {
        RubyNumeric i = this;
        while (true) {
            if (i.callMethod("<", to).isTrue()) {
                break;
            }
            getRuntime().yield(i);
            i = (RubyNumeric) i.callMethod("-", RubyFixnum.one(getRuntime()));
        }
        return this;
    }

    public RubyBoolean int_p() {
        return getRuntime().getTrue();
    }

    public IRubyObject step(RubyNumeric to, RubyNumeric step) {
        RubyNumeric i = this;
        if (step.getLongValue() == 0) {
            throw new ArgumentError(getRuntime(), "step cannot be 0");
        }

        String cmp = "<";
        if (((RubyBoolean) step.callMethod("<", RubyFixnum.newFixnum(getRuntime(), 0))).isFalse()) {
            cmp = ">";
        }

        while (true) {
            if (i.callMethod(cmp, to).isTrue()) {
                break;
            }
            getRuntime().yield(i);
            i = (RubyNumeric) i.callMethod("+", step);
        }
        return this;
    }

    public IRubyObject times() {
        RubyNumeric i = RubyFixnum.zero(getRuntime());
        while (true) {
            if (!i.callMethod("<", this).isTrue()) {
                break;
            }
            getRuntime().yield(i);
            i = (RubyNumeric) i.callMethod("+", RubyFixnum.one(getRuntime()));
        }
        return this;
    }

    public IRubyObject succ() {
        return callMethod("+", RubyFixnum.one(getRuntime()));
    }

    public IRubyObject upto(RubyNumeric to) {
        RubyNumeric i = this;
        while (true) {
            if (i.callMethod(">", to).isTrue()) {
                break;
            }
            getRuntime().yield(i);
            i = (RubyNumeric) i.callMethod("+", RubyFixnum.one(getRuntime()));
        }
        return this;
    }

    public RubyInteger to_i() {
        return this;
    }
}
