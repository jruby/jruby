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

import java.util.HashMap;
import java.util.Map;

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

    private Map rubyThreadList = new HashMap();
    private boolean enclosed = false;

    public static RubyClass createThreadGroupClass(Ruby runtime) {
        RubyClass threadGroupClass = runtime.defineClass("ThreadGroup", 
                runtime.getClasses().getObjectClass());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyThreadGroup.class);
        
        threadGroupClass.defineMethod("add",
        		callbackFactory.getMethod("add", RubyThread.class));
        threadGroupClass.defineMethod("enclose",
        		callbackFactory.getMethod("enclose"));
        threadGroupClass.defineMethod("enclosed?",
        		callbackFactory.getMethod("isEnclosed"));
        threadGroupClass.defineMethod("list",
        		callbackFactory.getMethod("list"));
        threadGroupClass.defineSingletonMethod("new",
                callbackFactory.getSingletonMethod("newInstance"));
        
        // create the default thread group
        RubyThreadGroup defaultThreadGroup = new RubyThreadGroup(runtime, threadGroupClass);
        threadGroupClass.defineConstant("Default", defaultThreadGroup);

        return threadGroupClass;
    }
    
    public static IRubyObject newInstance(IRubyObject recv) {
        return new RubyThreadGroup(recv.getRuntime(), (RubyClass)recv);
    }

    public IRubyObject add(RubyThread rubyThread) {
    	if (isFrozen()) {
        	throw getRuntime().newTypeError("can't add to frozen ThreadGroup");
    	}
    	
    	if (rubyThread.group() != getRuntime().getNil()) {
    		RubyThreadGroup threadGroup = (RubyThreadGroup)rubyThread.group();
    		threadGroup.rubyThreadList.remove(new Integer(System.identityHashCode(rubyThread)));
    	}
    	
    	rubyThread.setThreadGroup(this);
		rubyThreadList.put(new Integer(System.identityHashCode(rubyThread)), rubyThread);
	
		return this;
    }
    
    public void remove(RubyThread rubyThread) {
    	rubyThread.setThreadGroup(null);
    	rubyThreadList.remove(new Integer(System.identityHashCode(rubyThread)));
    }
    
    public IRubyObject enclose() {
    	enclosed = true;
    	
    	return this;
    }
    
    public IRubyObject isEnclosed() {
    	return new RubyBoolean(getRuntime(), enclosed);
    }
    
    public IRubyObject list() {
    	return getRuntime().newArray((IRubyObject[])rubyThreadList.values().toArray(new IRubyObject[rubyThreadList.size()]));
    }

    private RubyThreadGroup(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

}
