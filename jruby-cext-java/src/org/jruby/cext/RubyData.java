/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;

public class RubyData extends RubyObject {

    private RubyData(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public static final RubyData newRubyData(Ruby runtime, RubyClass klass, long handle) {

        RubyData d = new RubyData(runtime, klass);
        GC.register(d, new Handle(runtime, handle));

        return d;
    }
}
