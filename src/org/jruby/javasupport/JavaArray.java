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

package org.jruby.javasupport;

import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.exceptions.TypeError;
import org.jruby.exceptions.ArgumentError;

public class JavaArray extends JavaObject implements IndexCallable {

    public JavaArray(Ruby runtime, Object array) {
        super(runtime, runtime.getClasses().getJavaArrayClass(), array);
        Asserts.isTrue(array.getClass().isArray());
    }

    public static RubyClass createJavaArrayClass(Ruby runtime) {
        RubyClass javaArrayClass =
                runtime.defineClass("JavaArray", runtime.getClasses().getJavaObjectClass());
        return javaArrayClass;
    }

    public RubyFixnum length() {
        int length = ((Object[]) getValue()).length;
        return RubyFixnum.newFixnum(getRuntime(), length);
    }

    public IRubyObject aref(IRubyObject index) {
        if (! (index instanceof RubyInteger)) {
            throw new TypeError(getRuntime(), index, getRuntime().getClasses().getIntegerClass());
        }
        int intIndex = (int) ((RubyInteger) index).getLongValue();
        Object[] array = ((Object[]) getValue());
        if (intIndex < 0 || intIndex >= array.length) {
            throw new ArgumentError(getRuntime(),
                                    "index out of bounds for java array (" + intIndex +
                                    " for length " + array.length + ")");
        }
        Object result = array[intIndex];
        if (result == null) {
            return getRuntime().getNil();
        }
        return JavaObject.wrap(getRuntime(), result);
    }


    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            default :
                return super.callIndexed(index, args);
        }
    }
}
