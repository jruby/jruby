/*
 * RubyMethod.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
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
package org.jruby;

import java.lang.ref.*;
import java.util.*;

import org.jruby.core.*;

public class RubyObjectSpace {

    /** Create the ObjectSpace module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createObjectSpaceModule(Ruby ruby) {
        RubyCallbackMethod each_object =
            new ReflectionCallbackMethod(RubyObjectSpace.class, "each_object", true, true);

        RubyModule objectSpaceModule = ruby.defineModule("ObjectSpace");

        objectSpaceModule.defineModuleFunction("each_object", each_object);

        return objectSpaceModule;
    }

    public static RubyObject each_object(
        Ruby ruby,
        RubyObject recv,
        RubyObject[] args) {
        if (args.length == 1) {
            Iterator iter = new LinkedList(ruby.objectSpace).iterator();

            while (iter.hasNext()) {
                SoftReference ref = (SoftReference) iter.next();
                RubyObject obj = (RubyObject) ref.get();
                if (obj != null) {
                    if (obj instanceof RubyModule
                        && (((RubyModule) obj).isIncluded() || ((RubyModule) obj).isSingleton())) {
                        continue;
                    } else {
                        if (obj.m_kind_of((RubyModule)args[0]).isTrue()) {
                            ruby.yield(obj);
                        }
                    }
                } else {
                    ruby.objectSpace.remove(ref);
                }
            }
            return ruby.getNil();
        } else {
            Iterator iter = new LinkedList(ruby.objectSpace).iterator();

            while (iter.hasNext()) {
                SoftReference ref = (SoftReference) iter.next();
                RubyObject obj = (RubyObject) ref.get();
                if (obj != null) {
                    if (obj instanceof RubyModule
                        && (((RubyModule) obj).isIncluded() || ((RubyModule) obj).isSingleton())) {
                        continue;
                    } else {
                        ruby.yield(obj);
                    }
                } else {
                    ruby.objectSpace.remove(ref);
                }
            }
            return ruby.getNil();
        }
    }
}