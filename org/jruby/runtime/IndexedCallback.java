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
import org.jruby.RubyObject;
import org.jruby.util.Asserts;

/**
 * Implements callback on built-in Ruby methods using an integer index.
 *
 */

public class IndexedCallback implements Callback {
    private final int index;
    private final int arity;

    private IndexedCallback(int index, int arity) {
        this.index = index;
        this.arity = arity;
    }

    public static IndexedCallback create(int index, int arity) {
        return new IndexedCallback(index, arity);
    }

    public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
        checkArity(args);
        return ((IndexCallable) recv).callIndexed(index, args);
    }

    public int getArity() {
        return arity;
    }

    private void checkArity(RubyObject[] args) {
        if (arity >= 0) {
            Asserts.assertTrue(args.length == arity,
                               "Expected " + arity + ", got " + args.length);
        }
    }
}
