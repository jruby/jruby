/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.libraries;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.CallbackFactory;

public class Mutex extends RubyObject {
    private boolean isLocked = false;

    private Mutex(Ruby runtime) {
        super(runtime, runtime.getClass("Mutex"));
    }

    public static void createMutexClass(Ruby runtime) {
        RubyClass mutexClass =
                runtime.defineClass("Mutex", runtime.getClasses().getObjectClass());
        CallbackFactory callbackFactory = runtime.callbackFactory();
        mutexClass.defineMethod("lock", callbackFactory.getMethod(Mutex.class, "lock"));
        mutexClass.defineMethod("unlock", callbackFactory.getMethod(Mutex.class, "unlock"));
        mutexClass.defineMethod("locked?", callbackFactory.getMethod(Mutex.class, "locked_p"));
    }

    public Mutex lock() {
        isLocked = true;
        return this;
    }

    public Mutex unlock() {
        isLocked = false;
        return this;
    }

    public RubyBoolean locked_p() {
        return getRuntime().newBoolean(isLocked);
    }
}
