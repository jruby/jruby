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

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.RubyBoolean;
import org.jruby.runtime.builtin.IRubyObject;

public class NullJavaObject extends JavaObject {

    public NullJavaObject(Ruby runtime) {
        super(runtime, null);
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isJavaNull() {
        return true;
    }

    public RubyString to_s() {
        return RubyString.newString(getRuntime(), "null");
    }

    public RubyBoolean equal(IRubyObject other) {
        return RubyBoolean.newBoolean(getRuntime(), other instanceof NullJavaObject);
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(runtime, 0);
    }
}
