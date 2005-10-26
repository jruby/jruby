/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
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
 */
public class RubyThreadGroup extends RubyObject {
    private Map rubyThreadList = new HashMap();
    private boolean enclosed = false;

    public static RubyClass createThreadGroupClass(IRuby runtime) {
        RubyClass threadGroupClass = runtime.defineClass("ThreadGroup", runtime.getObject());
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

    private RubyThreadGroup(IRuby runtime, RubyClass type) {
        super(runtime, type);
    }

}
