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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import java.util.Iterator;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyObjectSpace {

    /** Create the ObjectSpace module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createObjectSpaceModule(IRuby runtime) {
        RubyModule objectSpaceModule = runtime.defineModule("ObjectSpace");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyObjectSpace.class);
        objectSpaceModule.defineModuleFunction("each_object", callbackFactory.getOptSingletonMethod("each_object"));
        objectSpaceModule.defineFastModuleFunction("garbage_collect", callbackFactory.getSingletonMethod("garbage_collect"));
        objectSpaceModule.defineFastModuleFunction("_id2ref", callbackFactory.getSingletonMethod("id2ref", RubyFixnum.class));
        objectSpaceModule.defineModuleFunction("define_finalizer", 
        		callbackFactory.getOptSingletonMethod("define_finalizer"));
        objectSpaceModule.defineModuleFunction("undefine_finalizer", 
                callbackFactory.getOptSingletonMethod("undefine_finalizer"));

        return objectSpaceModule;
    }

    // FIXME: Figure out feasibility of this...
    public static IRubyObject define_finalizer(IRubyObject recv, IRubyObject[] args) {
        // Put in to fake tempfile.rb out.
        recv.getRuntime().getWarnings().warn("JRuby does not currently support defining finalizers");
        return recv;
    }

    // FIXME: Figure out feasibility of this...
    public static IRubyObject undefine_finalizer(IRubyObject recv, IRubyObject[] args) {
        // Put in to fake other stuff out.
        recv.getRuntime().getWarnings().warn("JRuby does not currently support defining finalizers");
        return recv;
    }

    public static IRubyObject id2ref(IRubyObject recv, RubyFixnum id) {
        IRuby runtime = id.getRuntime();
        long longId = id.getLongValue();
        if (longId == 0) {
            return runtime.getFalse();
        } else if (longId == 2) {
            return runtime.getTrue();
        } else if (longId == 4) {
            return runtime.getNil();
        } else if (longId % 2 != 0) { // odd
            return runtime.newFixnum((longId - 1) / 2);
        } else {
            IRubyObject object = runtime.getObjectSpace().id2ref(longId);
            if (object == null)
                runtime.newRangeError("not an id value");
            return object;
        }
    }
    
    public static IRubyObject each_object(IRubyObject recv, IRubyObject[] args) {
        RubyModule rubyClass;
        if (args.length == 0) {
            rubyClass = recv.getRuntime().getObject();
        } else {
            rubyClass = (RubyModule) args[0];
        }
        int count = 0;
        Iterator iter = recv.getRuntime().getObjectSpace().iterator(rubyClass);
        IRubyObject obj = null;
        ThreadContext context = recv.getRuntime().getCurrentContext();
        while ((obj = (IRubyObject)iter.next()) != null) {
            count++;
            context.yield(obj);
        }
        return recv.getRuntime().newFixnum(count);
    }

    public static IRubyObject garbage_collect(IRubyObject recv) {
        return RubyGC.start(recv);
    }
}
