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

import org.jruby.exceptions.*;
import org.jruby.runtime.*;

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

        integerClass.defineSingletonMethod("induced_from", CallbackFactory.getSingletonMethod(RubyInteger.class, "induced_from", RubyObject.class));

        integerClass.defineMethod("chr", CallbackFactory.getMethod(RubyInteger.class, "chr"));
        integerClass.defineMethod("integer?", CallbackFactory.getMethod(RubyInteger.class, "int_p"));
        integerClass.defineMethod("to_i", CallbackFactory.getSelfMethod());
        integerClass.defineMethod("to_int", CallbackFactory.getSelfMethod());
        integerClass.defineMethod("ceil", CallbackFactory.getSelfMethod());
        integerClass.defineMethod("floor", CallbackFactory.getSelfMethod());
        integerClass.defineMethod("round", CallbackFactory.getSelfMethod());
        integerClass.defineMethod("truncate", CallbackFactory.getSelfMethod());

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
        return RubyFloat.newFloat(ruby, getDoubleValue());
    }
    
    
    // Integer methods

    public static RubyInteger induced_from(Ruby ruby, RubyObject recv, RubyObject number) {
        if (number instanceof RubyNumeric) {
            return (RubyInteger) number.funcall("to_i");
        } else {
            throw new TypeError(ruby, "failed to convert " + number.getRubyClass() + " into Integer");
        }
    }

    public RubyString chr() {
        if (getLongValue() < 0 || getLongValue() > 0xff) {
            throw new RangeError(getRuby(), this.toString() + " out of char range");
        }
        return RubyString.newString(getRuby(), new String(new char[] {(char) getLongValue()}));
    }

    public RubyObject downto(RubyNumeric to) {
        RubyNumeric i = this;
        while (true) {
            if (((RubyBoolean) i.funcall("<", to)).isTrue()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric) i.funcall("-", RubyFixnum.one(getRuby()));
        }
        return this;
    }

    public RubyBoolean int_p() {
        return getRuby().getTrue();
    }

    public RubyObject step(RubyNumeric to, RubyNumeric step) {
        RubyNumeric i = this;
        if (step.getLongValue() == 0) {
            throw new ArgumentError(getRuby(), "step cannot be 0");
        }

        String cmp = "<";
        if (((RubyBoolean) step.funcall("<", RubyFixnum.newFixnum(getRuby(), 0))).isFalse()) {
            cmp = ">";
        }

        while (true) {
            if (((RubyBoolean) i.funcall(cmp, to)).isTrue()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric) i.funcall("+", step);
        }
        return this;
    }

    public RubyObject times() {
        RubyNumeric i = RubyFixnum.zero(getRuby());
        while (true) {
            if (i.funcall("<", this).isFalse()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric) i.funcall("+", RubyFixnum.one(getRuby()));
        }
        return this;
    }

    public RubyObject succ() {
        return funcall("+", RubyFixnum.one(getRuby()));
    }

    public RubyObject upto(RubyNumeric to) {
        RubyNumeric i = this;
        while (true) {
            if (i.funcall(">", to).isTrue()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric) i.funcall("+", RubyFixnum.one(getRuby()));
        }
        return this;
    }

    public RubyInteger to_i() {
        return this;
    }
}
