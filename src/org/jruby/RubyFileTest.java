/*
 * RubyFileTest.java - No description
 * Created on 04.05.2004, 18:00:54
 *
 * Copyright (C) 2004 Thomas E Enebo
 * Thomas E Enebo <enebo@acm.rog>
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
        CallbackFactory callbackFactory = runtime.callbackFactory();

        fileTestModule.defineMethod("directory?", callbackFactory.getSingletonMethod(RubyFileTest.class, "directory_p", RubyString.class));
        fileTestModule.defineMethod("exist?", callbackFactory.getSingletonMethod(RubyFileTest.class, "exist_p", RubyString.class));
        fileTestModule.defineMethod("exists?", callbackFactory.getSingletonMethod(RubyFileTest.class, "exist_p", RubyString.class));
        fileTestModule.defineMethod("readable?", callbackFactory.getSingletonMethod(RubyFileTest.class, "readable_p", RubyString.class));
        fileTestModule.defineMethod("readable_real?", callbackFactory.getSingletonMethod(RubyFileTest.class, "readable_p", RubyString.class));
        fileTestModule.defineMethod("size", callbackFactory.getSingletonMethod(RubyFileTest.class, "size", RubyString.class));
        fileTestModule.defineMethod("writable?", callbackFactory.getSingletonMethod(RubyFileTest.class, "writable_p", RubyString.class));
        fileTestModule.defineMethod("writable_real?", callbackFactory.getSingletonMethod(RubyFileTest.class, "writable_p", RubyString.class));
        fileTestModule.defineMethod("zero?", callbackFactory.getSingletonMethod(RubyFileTest.class, "zero_p", RubyString.class));
        
        return fileTestModule;
    }
    
    public static RubyBoolean directory_p(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(recv.getRuntime(), 
                new File(filename.getValue()).isDirectory());
    }
    
    public static IRubyObject exist_p(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(recv.getRuntime(), 
                new File(filename.getValue()).exists());
    }

    // We do both readable and readable_real through the same method because
    // in our java process effective and real userid will always be the same.
    public static RubyBoolean readable_p(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(filename.getRuntime(), 
                new File(filename.getValue()).canRead());
    }
    
    public static IRubyObject size(IRubyObject recv, RubyString filename) {
        File file = new File(filename.getValue());
        
        if (file.exists() == false) {
            throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT",
                    "No such file: " + filename.getValue());
        }
        return RubyFixnum.newFixnum(filename.getRuntime(), file.length());
    }
    
    // We do both writable and writable_real through the same method because
    // in our java process effective and real userid will always be the same.
    public static RubyBoolean writable_p(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(filename.getRuntime(), 
                new File(filename.getValue()).canWrite());
    }
    
    public static RubyBoolean zero_p(IRubyObject recv, RubyString filename) {
        File file = new File(filename.getValue());
        return RubyBoolean.newBoolean(filename.getRuntime(),
                file.exists() == true && file.length() == 0L);
                
    }
    
}
