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
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;

public class JavaArray extends JavaObject implements IndexCallable {

    public JavaArray(Ruby runtime, Object array) {
        super(runtime, runtime.getClasses().getJavaArrayClass(), array);
        Asserts.isTrue(array.getClass().isArray());
    }

    private static final int LENGTH = 1001;

    public static RubyClass createJavaArrayClass(Ruby runtime) {
        RubyClass javaArrayClass =
                runtime.defineClass("JavaArray", runtime.getClasses().getJavaObjectClass());

        javaArrayClass.defineMethod("length", IndexedCallback.create(LENGTH, 0));

        return javaArrayClass;
    }

    public RubyFixnum length() {
        int length = ((Object[]) getValue()).length;
        return RubyFixnum.newFixnum(getRuntime(), length);
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case LENGTH :
                return length();
            default :
                return super.callIndexed(index, args);
        }
    }
}
