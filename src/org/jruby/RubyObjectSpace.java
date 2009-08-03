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

import static org.jruby.RubyEnumerator.enumeratorize;

import java.util.Iterator;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyModule(name="ObjectSpace")
public class RubyObjectSpace {

    /** Create the ObjectSpace module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createObjectSpaceModule(Ruby runtime) {
        RubyModule objectSpaceModule = runtime.defineModule("ObjectSpace");
        runtime.setObjectSpaceModule(objectSpaceModule);
        
        objectSpaceModule.defineAnnotatedMethods(RubyObjectSpace.class);

        return objectSpaceModule;
    }

    @JRubyMethod(name = "define_finalizer", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject define_finalizer(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        IRubyObject finalizer = null;
        if (args.length == 2) {
            finalizer = args[1];
            if (!finalizer.respondsTo("call")) {
                throw runtime.newArgumentError("wrong type argument "
                        + finalizer.getType() + " (should be callable)");
            }
        } else {
            finalizer = runtime.newProc(Block.Type.PROC, block);
        }
        IRubyObject obj = args[0];
        runtime.getObjectSpace().addFinalizer(obj, finalizer);
        return runtime.newArray(runtime.newFixnum(runtime.getSafeLevel()), finalizer);
    }

    @JRubyMethod(name = "undefine_finalizer", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject undefine_finalizer(IRubyObject recv, IRubyObject arg1, Block block) {
        recv.getRuntime().getObjectSpace().removeFinalizers(RubyNumeric.fix2long(arg1.id()));
        return recv;
    }

    @JRubyMethod(name = "_id2ref", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject id2ref(IRubyObject recv, IRubyObject id) {
        Ruby runtime = id.getRuntime();
        if (!(id instanceof RubyFixnum)) {
            throw recv.getRuntime().newTypeError(id, recv.getRuntime().getFixnum());
        }
        RubyFixnum idFixnum = (RubyFixnum) id;
        long longId = idFixnum.getLongValue();
        if (longId == 0) {
            return runtime.getFalse();
        } else if (longId == 2) {
            return runtime.getTrue();
        } else if (longId == 4) {
            return runtime.getNil();
        } else if (longId % 2 != 0) {
            // odd
            return runtime.newFixnum((longId - 1) / 2);
        } else {
            IRubyObject object = runtime.getObjectSpace().id2ref(longId);
            if (object == null) {
                return runtime.getNil();
            }
            return object;
        }
    }
    
    public static IRubyObject each_object(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyModule rubyClass;
        if (args.length == 0) {
            rubyClass = recv.getRuntime().getObject();
        } else {
            if (!(args[0] instanceof RubyModule)) throw recv.getRuntime().newTypeError("class or module required");
            rubyClass = (RubyModule) args[0];
        }
        Ruby runtime = recv.getRuntime();
        int count = 0;
        if (rubyClass != runtime.getClassClass()) {
            if (!runtime.isObjectSpaceEnabled()) {
                throw runtime.newRuntimeError("ObjectSpace is disabled; each_object will only work with Class, pass -X+O to enable");
            }
            Iterator iter = recv.getRuntime().getObjectSpace().iterator(rubyClass);
            
            IRubyObject obj = null;
            while ((obj = (IRubyObject)iter.next()) != null) {
                count++;
                block.yield(context, obj);
            }
        } else {
            Iterator iter = runtime.getObject().subclasses(true).iterator();
            
            while (iter.hasNext()) {
                IRubyObject obj = (IRubyObject)iter.next();
                if (obj instanceof RubyClass && ((RubyClass)obj).isIncluded()) {
                    continue;
                }
                count++;
                block.yield(context, obj);
            }
        }
        return recv.getRuntime().newFixnum(count);
    }

    @JRubyMethod(name = "each_object", optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject each_object19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return block.isGiven() ? each_object(context, recv, args, block) : enumeratorize(context.getRuntime(), recv, "each_object", args);
    }

    @JRubyMethod(name = "garbage_collect", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject garbage_collect(IRubyObject recv) {
        return RubyGC.start(recv);
    }
}
