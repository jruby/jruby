/*
 * RubyFileTest.java - No description
 * Created on 04.05.2004, 18:00:54
 *
 * Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * Thomas E Enebo <enebo@acm.rog>
 * Charles O Nutter <headius@headius.com>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby;

import java.io.File;

import org.jruby.exceptions.ErrnoError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyFileTest {
    public static RubyModule createFileTestModule(Ruby runtime) {
        RubyModule fileTestModule = runtime.defineModule("FileTest");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFileTest.class);

        fileTestModule.defineSingletonMethod("directory?", callbackFactory.getSingletonMethod("directory_p", RubyString.class));
        fileTestModule.defineSingletonMethod("exist?", callbackFactory.getSingletonMethod("exist_p", RubyString.class));
        fileTestModule.defineSingletonMethod("exists?", callbackFactory.getSingletonMethod("exist_p", RubyString.class));
        fileTestModule.defineSingletonMethod("readable?", callbackFactory.getSingletonMethod("readable_p", RubyString.class));
        fileTestModule.defineSingletonMethod("readable_real?", callbackFactory.getSingletonMethod("readable_p", RubyString.class));
        fileTestModule.defineSingletonMethod("size", callbackFactory.getSingletonMethod("size", RubyString.class));
        fileTestModule.defineSingletonMethod("writable?", callbackFactory.getSingletonMethod("writable_p", RubyString.class));
        fileTestModule.defineSingletonMethod("writable_real?", callbackFactory.getSingletonMethod("writable_p", RubyString.class));
        fileTestModule.defineSingletonMethod("zero?", callbackFactory.getSingletonMethod("zero_p", RubyString.class));
        
        fileTestModule.defineMethod("directory?", callbackFactory.getSingletonMethod("directory_p", RubyString.class));
        fileTestModule.defineMethod("exist?", callbackFactory.getSingletonMethod("exist_p", RubyString.class));
        fileTestModule.defineMethod("exists?", callbackFactory.getSingletonMethod("exist_p", RubyString.class));
        fileTestModule.defineMethod("readable?", callbackFactory.getSingletonMethod("readable_p", RubyString.class));
        fileTestModule.defineMethod("readable_real?", callbackFactory.getSingletonMethod("readable_p", RubyString.class));
        fileTestModule.defineMethod("size", callbackFactory.getSingletonMethod("size", RubyString.class));
        fileTestModule.defineMethod("writable?", callbackFactory.getSingletonMethod("writable_p", RubyString.class));
        fileTestModule.defineMethod("writable_real?", callbackFactory.getSingletonMethod("writable_p", RubyString.class));
        fileTestModule.defineMethod("zero?", callbackFactory.getSingletonMethod("zero_p", RubyString.class));
        
        return fileTestModule;
    }
    
    public static RubyBoolean directory_p(IRubyObject recv, RubyString filename) {
        return recv.getRuntime().newBoolean(
                new File(filename.getValue()).isDirectory());
    }
    
    public static IRubyObject exist_p(IRubyObject recv, RubyString filename) {
        return recv.getRuntime().newBoolean(
                new File(filename.getValue()).exists());
    }

    // We do both readable and readable_real through the same method because
    // in our java process effective and real userid will always be the same.
    public static RubyBoolean readable_p(IRubyObject recv, RubyString filename) {
        return filename.getRuntime().newBoolean(
                new File(filename.getValue()).canRead());
    }
    
    public static IRubyObject size(IRubyObject recv, RubyString filename) {
        File file = new File(filename.getValue());
        
        if (!file.exists()) {
            throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT",
                    "No such file: " + filename.getValue());
        }
        return filename.getRuntime().newFixnum(file.length());
    }
    
    // We do both writable and writable_real through the same method because
    // in our java process effective and real userid will always be the same.
    public static RubyBoolean writable_p(IRubyObject recv, RubyString filename) {
        return filename.getRuntime().newBoolean(
                new File(filename.getValue()).canWrite());
    }
    
    public static RubyBoolean zero_p(IRubyObject recv, RubyString filename) {
        File file = new File(filename.getValue());
        return filename.getRuntime().newBoolean(
                file.exists() && file.length() == 0L);
                
    }
    
}
