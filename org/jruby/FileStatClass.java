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
package org.jruby;

import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.builtin.definitions.FileStatDefinition;

import java.io.File;

public class FileStatClass extends RubyObject implements IndexCallable {
    private File file;

    public static RubyClass createFileStatClass(Ruby runtime) {
        RubyClass fileStatClass = new FileStatDefinition(runtime).getType();
        return fileStatClass;
    }

    protected FileStatClass(Ruby runtime, File file) {
        super(runtime, runtime.getClasses().getFileStatClass());
        this.file = file;
    }

    public RubyBoolean directory_p() {
        return RubyBoolean.newBoolean(runtime, file.isDirectory());
    }


    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case FileStatDefinition.DIRECTORY_P :
                return directory_p();
        }
        return super.callIndexed(index, args);
    }
}
