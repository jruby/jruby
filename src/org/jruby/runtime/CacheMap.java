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
 * Copyright (C) 2005 Thomas E. Enebo <enebo@acm.org>
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
package org.jruby.runtime;

import java.util.WeakHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import org.jruby.RubyModule;
import org.jruby.util.collections.WeakHashSet;

/**
 * This class represents mappings between methods that have been cached and the classes which
 * have cached them.  Methods within RubyModule will update this cacheMap as needed.  Here is
 * a list of scenarios when cached methods will become invalid:
 * 
 *  1. Redefine a method in a base class
 *  2. Add an alias in a superclass that is the same name as a cached method in a base class
 *  3. Include a module that has a same-named method as one already caches in a base class
 *  4. Remove a method definition
 *  5. Add a same-named method in super class that has been cached in a super class
 *  
 * Concurrency is another concern with managing this structure.  Rather than synchronize this
 * we are going to rely on synchronization further upstream.  RubyModule methods that directly
 * call this is responsible for synchronization.
 */
public class CacheMap {
	Map mappings = new WeakHashMap();

	/**
	 * Add another class to the list of classes which are caching the method.
	 * 
	 * @param method which is cached
	 * @param module which is caching method
	 */
	public void add(ICallable method, RubyModule module) {
		Set classList = (Set) mappings.get(method);
		
		if (classList == null) {
			classList = new WeakHashSet();
			mappings.put(method, classList);
		}
		
		classList.add(module);
	}
	
	/**
	 * Remove all method caches associated with the provided method.  This signature
	 * relies on having the methods valid name passed with it since the caching classes
	 * store the cache by name.
	 * 
	 * @param name of the method to remove
	 * @param method to remove all caches of
	 */
	public void remove(String name, ICallable method) {
		Set classList = (Set) mappings.remove(method);
		
		// Removed method has never been used so it has not been cached
		if (classList == null) {
			return;
		}
		
		for(Iterator iter = classList.iterator(); iter.hasNext();) {
			RubyModule module = (RubyModule) iter.next();
            if (module != null) {
                module.removeCachedMethod(name);
            }
		}
	}
}
