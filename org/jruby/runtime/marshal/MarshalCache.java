/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.runtime.marshal;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubySymbol;
import org.jruby.Ruby;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.TypeError;

import java.util.Map;
import java.util.HashMap;

public class MarshalCache {
    private final Ruby runtime;
    private Map linkCache = new HashMap();
    private Map symbolCache = new HashMap();

    public MarshalCache(Ruby runtime) {
        this.runtime = runtime;
    }

    public boolean isRegistered(IRubyObject value) {
        return selectCache(value).containsKey(value);
    }

    public void register(IRubyObject value) {
        selectCache(value).put(value, key(selectCache(value).size()));
    }

    public int registeredIndex(IRubyObject value) {
        return ((Integer) selectCache(value).get(value)).intValue();
    }

    public IRubyObject linkedByIndex(int index) {
        if (! linkCache.containsKey(key(index))) {
            throw new ArgumentError(runtime, "dump format error (unlinked: " + index + ")");
        }
        return (IRubyObject) linkCache.get(key(index));
    }

    public IRubyObject symbolByIndex(int index) {
        if (! symbolCache.containsKey(key(index))) {
            throw new TypeError(runtime, "bad symbol");
        }
        return (RubySymbol) symbolCache.get(key(index));
    }

    private Map selectCache(IRubyObject value) {
        return (value instanceof RubySymbol) ? symbolCache : linkCache;
    }

    private Integer key(int index) {
        return new Integer(index);
    }
}
