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

import org.jruby.util.Asserts;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implements callback on built-in Ruby methods using an integer index.
 *
 * The class implementing the method uses this index to determine
 * what method to call, usually with a callIndexed(index, args) method
 * containing a switch statement with cases for all the methods it knows about.
 *
 * This is less flexible than reflection callbacks, that only need a name to
 * do dispatch, but is usually much faster.
 */
public final class IndexedCallback implements Callback {
    private final int index;
    private final Arity arity;

    private IndexedCallback(int index, Arity arity) {
        this.index = index;
        this.arity = arity;
    }

    /**
     * Create a callback with a fixed # of arguments
     */
    public static IndexedCallback create(int index, int arity) {
        return new IndexedCallback(index, Arity.fixed(arity));
    }

    /**
     * Create a callback with an optional # of arguments
     */
    public static IndexedCallback createOptional(int index) {
        return new IndexedCallback(index, Arity.optional());
    }

    /**
     * Create a callback with a minimal # of arguments
     */
    public static IndexedCallback createOptional(int index, int minimum) {
        Asserts.isTrue(minimum >= 0);
        return new IndexedCallback(index, Arity.required(minimum));
    }

    public IRubyObject execute(IRubyObject recv, IRubyObject args[]) {
        arity.checkArity(recv.getRuntime(), args);
        return ((IndexCallable) recv).callIndexed(index, args);
    }

    public Arity getArity() {
        return arity;
    }
}
