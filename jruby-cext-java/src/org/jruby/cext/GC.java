/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakIdentityHashMap;


public class GC {
    private static final Map<IRubyObject, Boolean> permRefs = new IdentityHashMap<IRubyObject, Boolean>();
    private static final Map<IRubyObject, Boolean> strongRefs = new IdentityHashMap<IRubyObject, Boolean>();
    @SuppressWarnings(value="unchecked")
    private static final Map<IRubyObject, Handle> allRefs = new WeakIdentityHashMap();
    @SuppressWarnings(value="unchecked")
    private static final Map<IRubyObject, Handle> dataRefs = new WeakIdentityHashMap();

    /**
     * This is an upcall from the C++ stub to mark objects that are only strongly 
     * reachable from a C VALUE instance.
     * 
     * @param obj The object to mark
     */

    public static final void mark(IRubyObject obj) {
        strongRefs.put(obj, Boolean.TRUE);
    }

    static final Handle lookup(IRubyObject obj) {
        return allRefs.get(obj);
    }

    /**
     * Called from Handle.valueOf
     * @param obj
     */
    static final void register(IRubyObject obj, Handle h) {
        allRefs.put(obj, h);

        if (obj instanceof RubyData) {
            dataRefs.put(obj, h);
        }

        strongRefs.put(obj, Boolean.TRUE);
    }

    static final void cleanup(ThreadContext context) {
        Map<IRubyObject, Boolean> tmp = new IdentityHashMap<IRubyObject, Boolean>(strongRefs);

        strongRefs.clear();
        
        //
        // Iterate over all the registered references, calling down to C++
        // to mark any objects only reachable from C code.
        //
        for (Handle h : new ArrayList<Handle>(dataRefs.values())) {
            Native.getInstance(context.getRuntime()).markHandle(h.getAddress());
        }
    }
}
