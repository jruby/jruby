/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * GC (Garbage Collection) Module
 *
 * Note: Since we rely on Java's memory model we can't provide the
 * kind of control over garbage collection that MRI provides.
 *
 * @author Anders
 * @version $Revision$
 */
public class RubyGC {
    public static RubyModule createGCModule(Ruby runtime) {
        RubyModule result = runtime.defineModule("GC");
        CallbackFactory callbackFactory = runtime.callbackFactory();
        
        result.defineSingletonMethod("start", callbackFactory.getSingletonMethod(RubyGC.class, "start"));
        result.defineSingletonMethod("garbage_collect", callbackFactory.getSingletonMethod(RubyGC.class, "start"));
        
        return result;        
    }

    public static IRubyObject start(IRubyObject recv) {
        System.gc();
        return recv.getRuntime().getNil();
    }
}