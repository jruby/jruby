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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyFileTest {
    public static RubyModule createFileTestModule(Ruby ruby) {
        RubyModule fileTestModule = ruby.defineModule("FileTest");
        CallbackFactory callbackFactory = ruby.callbackFactory();

        fileTestModule.defineMethod("directory?", callbackFactory.getSingletonMethod(RubyFileTest.class, "directory_p", RubyString.class));
        fileTestModule.defineMethod("exist?", callbackFactory.getSingletonMethod(RubyFileTest.class, "exist_p", RubyString.class));
        fileTestModule.defineMethod("exists?", callbackFactory.getSingletonMethod(RubyFileTest.class, "exist_p", RubyString.class));
        fileTestModule.defineMethod("writable?", callbackFactory.getSingletonMethod(RubyFileTest.class, "writable_p", RubyString.class));
        
        return fileTestModule;
    }
    
    public static RubyBoolean directory_p(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(recv.getRuntime(), new File(filename.toString()).isDirectory());
    }
    
    public static IRubyObject exist_p(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(recv.getRuntime(), new File(filename.toString()).exists());
    }
    
    public static RubyBoolean writable_p(IRubyObject recv, RubyString filename) {
        File file = new File(filename.getValue());
        
        return RubyBoolean.newBoolean(filename.getRuntime(), file.exists());
    }
}
