/*
 * Copyright (C) 2004 Dvaid Corbin <dcorbin@users.sourceforge.net>
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
package org.jruby.javasupport;

import java.lang.reflect.AccessibleObject;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class JavaAccessibleObject extends RubyObject {

	protected JavaAccessibleObject(Ruby runtime, RubyClass rubyClass) {
		super(runtime, rubyClass);
	}

	public static void registerRubyMethods(Ruby runtime, RubyClass result) {
        CallbackFactory callbackFactory = runtime.callbackFactory();

        result.defineMethod("accessible?", 
                callbackFactory.getMethod(JavaAccessibleObject.class, "isAccessible"));
        result.defineMethod("accessible=", 
                callbackFactory.getMethod(JavaAccessibleObject.class, "setAccessible", 
                        IRubyObject.class));
        
	}
	protected abstract AccessibleObject accesibleObject();
	
	public RubyBoolean isAccessible() {
		return new RubyBoolean(getRuntime(),accesibleObject().isAccessible());
	}

	public IRubyObject setAccessible(IRubyObject object) {
	    accesibleObject().setAccessible(object.isTrue());
		return object;
	}

}
