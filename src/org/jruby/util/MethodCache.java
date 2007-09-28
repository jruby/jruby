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
 * Copyright (C) 2007 Ye Zheng <dreamhead.cn@gmail.com>
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

package org.jruby.util;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;

public class MethodCache {

    public static class CacheEntry {
        public RubyModule klass = null;
        public String methodName = null;
        public DynamicMethod method = null;

        void put(RubyModule klass, String methodName, DynamicMethod method) {
            this.klass = klass;
            this.methodName = methodName;
            this.method = method;
        }

        void clear() {
            this.klass = null;
            this.methodName = null;
            this.method = null;
        }
    }


    private final static int CACHE_SIZE = 0x800;
    private final static int CACHE_MASK = 0x7ff;

    private final CacheEntry[] cache = new CacheEntry[CACHE_SIZE];
    private boolean initialized;

    public MethodCache() {
        initialized = false;

        for (int i = 0; i < CACHE_SIZE; i++ ) {
            cache[i] = new CacheEntry();
        }
    }
    
    public void initialized() {
        initialized = true;
    }
    
    public void clearCacheForModule(RubyModule module) {
        if (!initialized) {
            return;
        }
        for (int i = 0; i < CACHE_SIZE; i++ ) {
            if (cache[i].klass == module) {
                cache[i].clear();
            }
        }
    }

    public CacheEntry getMethod(RubyModule klass, String methodName) {       
        return cache[cacheIndex(klass, methodName)];
    }
    
    public void putMethod(RubyModule klass, String methodName, DynamicMethod method) {
        cache[cacheIndex(klass, methodName)].put(klass, methodName, method);
    }
    
    public void removeMethod(RubyClass c, String id) { 
        if (!initialized) {
            return;
        }
        
        for (int i = CACHE_SIZE; --i >= 0; ) {
            CacheEntry entry = cache[i];
            if (c == entry.klass && id.equals(entry.methodName)) {
                entry.methodName = null;
            }
        }
    }
    
    public void removeMethod(String methodName) {
        if (!initialized) {
            return;
        }
        
        for (int i = CACHE_SIZE; --i >= 0; ) {
            CacheEntry entry = cache[i];
            if (methodName.equals(entry.methodName)) {
                entry.methodName = null;
            }
        }
    }
    
    public void removeClass(RubyClass klass) {
        if (!initialized) {
            return;
        }
        
        for (int i = CACHE_SIZE; --i >= 0; ) {
            CacheEntry entry = cache[i]; 
            if (entry.klass == klass) {
                entry.methodName = null;
            }
        }
    }

    private int cacheIndex(RubyModule klass, String methodName) {
        return (((klass.hashCode() >> 3) ^ (methodName.hashCode())) & CACHE_MASK);
    }

}
