/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.util.Asserts;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The arity of a method is the number of arguments it takes.
 */
public class Arity {
    private final int value;

    private Arity(int value) {
        this.value = value;
    }

    public static Arity createArity(int value) {
        return new Arity(value);
    }

    public static Arity fixed(int arity) {
        Asserts.isTrue(arity >= 0);
        return createArity(arity);
    }

    public static Arity optional() {
        return createArity(-1);
    }

    public static Arity required(int minimum) {
        Asserts.isTrue(minimum >= 0);
        return createArity(-(1 + minimum));
    }

    public static Arity noArguments() {
        return createArity(0);
    }

    public static Arity singleArgument() {
        return createArity(1);
    }

    public int getValue() {
        return value;
    }

    public void checkArity(Ruby ruby, IRubyObject[] args) {
        if (isFixed()) {
            if (args.length != required()) {
                throw new ArgumentError(ruby,
                                        "wrong # of arguments(" + args.length + " for " + required() + ")");
            }
        } else {
            if (args.length < required()) {
                throw new ArgumentError(ruby, "wrong # of arguments(at least " + required() + ")");
            }
        }
    }

    private boolean isFixed() {
        return (value >= 0);
    }

    private int required() {
        if (value < 0) {
            return -(1 + value);
        } else {
            return value;
        }
    }

}
