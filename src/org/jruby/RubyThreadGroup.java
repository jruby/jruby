/*
 *  Copyright (C) 2004 Charles O Nutter
 * 
 * Charles O Nutter <headius@headius.com>
 *
 *  JRuby - http://jruby.sourceforge.net
 *
 *  This file is part of JRuby
 *
 *  JRuby is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  JRuby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with JRuby; if not, write to
 *  the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307 USA
 */
package org.jruby;

import java.util.ArrayList;
import java.util.List;

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implementation of Ruby's <code>ThreadGroup</code> class. This is currently
 * just a stub.
 * <p>
 *
 * @author Charles O Nutter (headius@headius.com)
 * @version $Revision$
 */
public class RubyThreadGroup extends RubyObject {
    private static boolean globalAbortOnException; // move to runtime object

    private List rubyThreadList = new ArrayList();
    private boolean enclosed = false;

    public static RubyClass createThreadGroupClass(Ruby ruby) {
        RubyClass threadGroupClass = ruby.defineClass("ThreadGroup", 
                ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();
        
        threadGroupClass.defineMethod("add",
        		callbackFactory.getMethod(RubyThreadGroup.class, "add", ThreadClass.class));
        threadGroupClass.defineMethod("enclose",
        		callbackFactory.getMethod(RubyThreadGroup.class, "enclose"));
        threadGroupClass.defineMethod("enclosed?",
        		callbackFactory.getMethod(RubyThreadGroup.class, "isEnclosed"));
        threadGroupClass.defineMethod("list",
        		callbackFactory.getMethod(RubyThreadGroup.class, "list"));
        threadGroupClass.defineSingletonMethod("new",
                callbackFactory.getSingletonMethod(RubyThreadGroup.class, "newInstance"));
        
        // create the default thread group
        RubyThreadGroup defaultThreadGroup = new RubyThreadGroup(ruby, threadGroupClass);
        threadGroupClass.defineConstant("Default", defaultThreadGroup);

        return threadGroupClass;
    }
    
    public static IRubyObject newInstance(IRubyObject recv) {
        return new RubyThreadGroup(recv.getRuntime(), (RubyClass)recv);
    }

    public IRubyObject add(ThreadClass rubyThread) {
    	if (isFrozen()) {
        	throw new TypeError(getRuntime(), "can't add to frozen ThreadGroup");
    	}
    	
		rubyThreadList.add(rubyThread);
	
		return this;
    }
    
    public IRubyObject enclose() {
    	enclosed = true;
    	
    	return this;
    }
    
    public IRubyObject isEnclosed() {
    	return new RubyBoolean(getRuntime(), enclosed);
    }
    
    public IRubyObject list() {
    	return RubyArray.newArray(getRuntime(), rubyThreadList);
    }

    private RubyThreadGroup(Ruby ruby, RubyClass type) {
        super(ruby, type);
    }

}
