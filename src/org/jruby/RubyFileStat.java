/*
 * Copyright (C) 2002 Anders Bengtsson
 * Copyright (C) 2004 Charles O Nutter
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Charles O Nutter <headius@headius.com>
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

import java.io.File;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * note: renamed from FileStatClass.java
 * Implements File::Stat
 */
public class RubyFileStat extends RubyObject {
    private File file;
    private static final int READ = 222;
    private static final int WRITE = 444;

    public static RubyClass createFileStatClass(Ruby runtime) {
        RubyClass fileStatClass = runtime.defineClass("FileStat", runtime.getClasses().getObjectClass());
    	
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFileStat.class);

        fileStatClass.defineMethod("directory?", callbackFactory.getMethod("directory_p"));
        fileStatClass.defineMethod("mode", callbackFactory.getMethod("mode"));
        fileStatClass.defineMethod("size", callbackFactory.getMethod("size"));
        fileStatClass.defineMethod("writable?", callbackFactory.getMethod("writable"));
    	
        return fileStatClass;
    }

    protected RubyFileStat(Ruby runtime, File file) {
        super(runtime, runtime.getClasses().getFileStatClass());
        this.file = file;
    }

    public RubyBoolean directory_p() {
        return getRuntime().newBoolean(file.isDirectory());
    }
    
    public IRubyObject mode() {
    	// implementation to lowest common denominator...Windows has no file mode, but C ruby returns either 0100444 or 0100666
    	int baseMode = 0100000;
    	if (file.canRead()) {
    		baseMode += READ;
    	}
    	
    	if (file.canWrite()) {
    		baseMode += WRITE;
    	}
    	
    	return getRuntime().newFixnum(baseMode);
    }
    
    public IRubyObject size() {
    	return getRuntime().newFixnum(file.length());
    }
    
    public IRubyObject writable() {
    	return getRuntime().newBoolean(file.canWrite());
    }
}
