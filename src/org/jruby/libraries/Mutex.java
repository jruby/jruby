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
package org.jruby.libraries;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.builtin.IRubyObject;

public class Mutex extends RubyObject implements IndexCallable {
    private boolean isLocked = false;

    private Mutex(Ruby runtime) {
        super(runtime, runtime.getClass("Mutex"));
    }

    private static final int LOCK = 1;
    private static final int UNLOCK = 2;
    private static final int LOCKED_P = 3;

    private static final int NEW = 4 | 0x100;

    public static void createMutexClass(Ruby runtime) {
        RubyClass mutexClass =
                runtime.defineClass("Mutex", runtime.getClasses().getObjectClass());

        mutexClass.defineMethod("lock", IndexedCallback.create(LOCK, 0));
        mutexClass.defineMethod("unlock", IndexedCallback.create(UNLOCK, 0));
        mutexClass.defineMethod("locked?", IndexedCallback.create(LOCKED_P, 0));
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
        return RubyBoolean.newBoolean(getRuntime(), isLocked);
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case LOCK :
                return lock();
            case UNLOCK :
                return unlock();
            case LOCKED_P :
                return locked_p();
            default :
                return super.callIndexed(index, args);
        }
    }
}
