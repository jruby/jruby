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
package org.jruby.internal.runtime.builtin.definitions;

import org.jruby.runtime.builtin.definitions.ClassDefinition;
import org.jruby.runtime.builtin.definitions.SingletonMethodContext;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.util.Asserts;

public class FileStatDefinition extends ClassDefinition {
    private static final int FILE_STAT = 0x1200; // todo: what constant should i use?
    public static final int DIRECTORY_P = FILE_STAT | 0x01;

    public FileStatDefinition(Ruby runtime) {
        super(runtime);
    }

    protected RubyClass createType(Ruby runtime) {
        return runtime.defineClass("File::Stat", runtime.getClasses().getObjectClass());
    }

    protected void defineSingletonMethods(SingletonMethodContext context) {
    }

    protected void defineMethods(MethodContext context) {
        context.create("directory?", DIRECTORY_P, 0);
    }

    public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
        Asserts.notReached();
        return null;
    }
}
