/*
 * RubyMethodCache.java - No description
 * Created on 10. January 2002, 15:05
 * 
 * Copyright (C) 2001,2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.internal.runtime.methods;

import java.util.*;
import org.jruby.*;

/**
 * A cache for searched method.
 * 
 * @author jpetersen
 * @version $Revision$
 * @since 0.3.1
 */
public class RubyMethodCache {
	private Ruby ruby;
	
	private Map methodCache = new HashMap();
	
	public RubyMethodCache(Ruby ruby) {
	    this.ruby = ruby;
	}

    /**
     * Put a "method is undefined" entry to the cache
     * 
     * @param recvClass The receiver module/class.
     * @param name The method name.
     */
	public void saveUndefinedEntry(RubyModule recvClass, String name) {
        methodCache.put(getKey(recvClass, name), new CacheEntry(name, recvClass));
    }

    /**
     * Put a method entry to the cache.
     * 
     * @param recvClass The receiver module/class.
     * @param name The method name.
     * @param entry The CacheEntry to save.
     */
    public void saveEntry(RubyModule recvClass, String name, CacheEntry entry) {
        methodCache.put(getKey(recvClass, name), entry);
    }

    /**
     * Receives a method entry from the cache
     * 
     * @param recvClass The receiver module/class.
     * @param name The method name.
     * @return CacheEntry The CacheEntry to save.
     */
    public CacheEntry getEntry(RubyModule recvClass, String name) {
        return (CacheEntry) methodCache.get(getKey(recvClass, name));
    }
    
    /**
     * Removes all methods named name from the cache.
     * 
     * @param name The name of the methods.
     */
    public void clearByName(String name) {
        Iterator iter = methodCache.values().iterator();
        while (iter.hasNext()) {
            CacheEntry entry = (CacheEntry) iter.next();
            if (entry.getName().equals(name)) {
                iter.remove();
            }
        }
    }

    /**
     * Removes all entries from the cache.
     */
    public void clear() {
        methodCache.clear();
    }
    
    /**
     * Create a hash key to save an entry.
     */
    private EntryKey getKey(RubyModule recvClass, String name) {
        return new EntryKey(recvClass, name);
    }

    private class EntryKey {
        private final RubyModule recvClass;
        private final String name;

        public EntryKey(RubyModule recvClass, String name) {
            this.recvClass = recvClass;
            this.name = name;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || (! (other instanceof EntryKey))) {
                return false;
            }
            EntryKey otherKey = (EntryKey) other;
            return (name.equals(otherKey.name) && recvClass == otherKey.recvClass);
        }

        public int hashCode() {
            return (System.identityHashCode(recvClass) ^ name.hashCode());
        }
    }
}
