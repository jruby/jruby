/*
 * RubyGC.java
 * Created on 27 May 2002
 * 
 * Copyright (C) 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Anders Bengtsson
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

    public static RubyModule createGCModule(Ruby ruby) {
        RubyModule gcModule = ruby.defineModule("GC");
        gcModule.defineSingletonMethod("start", CallbackFactory.getSingletonMethod(RubyGC.class, "start"));
        gcModule.defineAlias("garbage_collect", "start");
        return gcModule;
    }

    public static RubyObject start(Ruby ruby, RubyObject recv) {
        System.gc();
        return ruby.getNil();
    }
}
