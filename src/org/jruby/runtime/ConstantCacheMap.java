/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.Ruby;
import org.jruby.runtime.callsite.ConstantSite;
import org.jruby.util.collections.WeakHashSet;

public class ConstantCacheMap {
    private final AtomicInteger addCount = new AtomicInteger(0);
    private final AtomicInteger removeCount = new AtomicInteger(0);
    private final AtomicInteger flushTriggeredRemoveCount = new AtomicInteger(0);
    private final AtomicInteger flushCount = new AtomicInteger(0);
    private final Map<String, Set<ConstantSite>> mappings = new WeakHashMap<String, Set<ConstantSite>>();
    
    public ConstantCacheMap(Ruby ruby) {
    }
    
    public interface CacheSite {
        public void removeCachedMethod();
    }
    
    public int getAddCount() {
        return addCount.get();
    }
    
    public int getRemoveCount() {
        return removeCount.get();
    }
    
    public int getFlushCount() {
        return flushCount.get();
    }
    
    public synchronized void flush() {
        int totalRemoved = 0;
        for (String name : mappings.keySet()) {
            totalRemoved += remove(name);
        }
        
        flushTriggeredRemoveCount.addAndGet(totalRemoved);
        flushCount.incrementAndGet();
    }
    
    /**
     * Add another class to the list of classes which are caching the method.
     *
     * @param method which is cached
     * @param module which is caching method
     */
    public synchronized void add(String name, ConstantSite site) {
        Set<ConstantSite> siteList = mappings.get(name);
        
        if (siteList == null) {
            siteList = new WeakHashSet<ConstantSite>();
            mappings.put(name, siteList);
        }

        siteList.add(site);
        
        addCount.incrementAndGet();
    }
    
    /**
     * Remove all method caches associated with the provided method.
     * 
     * @param method to remove all caches of
     */
    public synchronized int remove(String name) {
        Set<ConstantSite> siteList = mappings.remove(name);

        if (siteList == null) return 0; // Not currently cached

        int totalRemoved = 0;
        for (ConstantSite site : siteList) {
            totalRemoved++;
            site.invalidate();
        }
        
        return removeCount.addAndGet(totalRemoved);
    }
}
