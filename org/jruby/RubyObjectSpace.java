/*
 * RubyObjectSpace.java - No description
 * Created on 04. November 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.runtime.*;

public class RubyObjectSpace {

    /** Create the ObjectSpace module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createObjectSpaceModule(Ruby ruby) {
        Callback each_object = CallbackFactory.getOptSingletonMethod(RubyObjectSpace.class, "each_object");

        RubyModule objectSpaceModule = ruby.defineModule("ObjectSpace");

        objectSpaceModule.defineModuleFunction("each_object", each_object);

        return objectSpaceModule;
    }

    public static RubyObject each_object(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyModule rubyClass;
        if (args.length == 0) {
            rubyClass = ruby.getClasses().getObjectClass();
        } else {
            rubyClass = (RubyModule) args[0];
        }
        Iterator iter = ruby.objectSpace.iterator(rubyClass);
        while (iter.hasNext()) {
            ruby.yield((RubyObject) iter.next());
        }
        return ruby.getNil();
    }
}
