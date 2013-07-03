/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 MenTaLguY <mental@rydia.net>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.thread;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The "ConditionVariable" class from the 'thread' library.
 */
@JRubyClass(name = "ConditionVariable")
public class ConditionVariable extends RubyObject {

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static ConditionVariable newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        ConditionVariable result = new ConditionVariable(context.runtime, (RubyClass) recv);
        result.callInit(context, args, block);
        return result;
    }

    public ConditionVariable(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static void setup(Ruby runtime) {
        RubyClass cConditionVariable = runtime.defineClass("ConditionVariable", runtime.getObject(), new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ConditionVariable(runtime, klass);
            }
        });
        cConditionVariable.setReifiedClass(ConditionVariable.class);
        cConditionVariable.defineAnnotatedMethods(ConditionVariable.class);
    }

    @JRubyMethod(name = "wait", required = 1, optional = 1)
    public IRubyObject wait_ruby(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        if (args.length < 1) {
            throw runtime.newArgumentError(args.length, 1);
        }
        if (args.length > 2) {
            throw runtime.newArgumentError(args.length, 2);
        }
        if (!(args[0] instanceof Mutex)) {
            throw context.runtime.newTypeError(args[0], runtime.getClass("Mutex"));
        }
        Mutex mutex = (Mutex) args[0];
        Double timeout = null;
        if (args.length > 1 && !args[1].isNil()) {
            timeout = args[1].convertToFloat().getDoubleValue();
            if (timeout < 0) {
                throw runtime.newArgumentError("time interval must be positive");
            }
        }
        if (Thread.interrupted()) {
            throw runtime.newConcurrencyError("thread interrupted");
        }
        boolean success = false;
        try {
            synchronized (this) {
                mutex.unlock(context);
                try {
                    success = context.getThread().wait_timeout(this, timeout);
                } catch (InterruptedException ie) {
                    throw runtime.newConcurrencyError(ie.getLocalizedMessage());
                } finally {
                    // An interrupt or timeout may have caused us to miss
                    // a notify that we consumed, so do another notify in
                    // case someone else is available to pick it up.
                    if (!success) {
                        this.notify();
                    }
                }
            }
        } finally {
            mutex.lock(context);
        }
        if (timeout != null) {
            return runtime.newBoolean(success);
        } else {
            return this;
        }
    }

    @JRubyMethod
    public synchronized IRubyObject broadcast(ThreadContext context) {
        notifyAll();
        return this;
    }

    @JRubyMethod
    public synchronized IRubyObject signal(ThreadContext context) {
        notify();
        return this;
    }
    
}
