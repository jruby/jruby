/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes;

import com.oracle.truffle.api.dsl.TypeSystem;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.runtime.Nil;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.math.BigInteger;

/**
 * This is a combination of generic, pack-specific, Truffle and JRuby types.
 */
@TypeSystem({
        Nil.class,
        boolean.class,
        int.class,
        long.class,
        BigInteger.class,
        float.class,
        double.class,
        ByteList.class,
        RubyString.class,
        RubyBignum.class,
        RubyArray.class,
        RubyNilClass.class,
        int[].class,
        long[].class,
        double[].class,
        Object[].class,
        IRubyObject[].class
})
public class PackTypes {

}
