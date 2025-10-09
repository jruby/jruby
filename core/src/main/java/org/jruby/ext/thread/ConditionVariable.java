/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jruby.RubyMarshal;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.ConcurrentLinkedQueue;

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

    public static RubyClass setup(ThreadContext context, RubyClass Thread, RubyClass Object) {
        return (RubyClass) Object.setConstant(context, "ConditionVariable",
                Thread.defineClassUnder(context, "ConditionVariable", Object, ConditionVariable::new).
                        reifiedClass(ConditionVariable.class).
                        defineMethods(context, ConditionVariable.class).
                        undefMethods(context, "initialize_copy"));
    }

    @JRubyMethod(name = "wait")
    public IRubyObject wait_ruby(ThreadContext context, IRubyObject m) {
        return wait_ruby(context, m, context.nil);
    }

    @JRubyMethod(name = "wait")
    public IRubyObject wait_ruby(ThreadContext context, IRubyObject m, IRubyObject t) {
        RubyThread thread = context.getThread();

        waiters.add(thread);
        try {
            sites(context).mutex_sleep.call(context, this, m, t);
        } finally {
            waiters.remove(thread);
        }

        return this;
    }

    @JRubyMethod
    public synchronized IRubyObject broadcast(ThreadContext context) {
        waiters.removeIf(waiter -> {
            waiter.interrupt();
            return true;
        });

        return this;
    }

    @JRubyMethod
    public synchronized IRubyObject signal(ThreadContext context) {
        RubyThread waiter = waiters.poll();

        if (waiter != null) {
            waiter.interrupt();
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject marshal_dump(ThreadContext context) {
        return RubyMarshal.undumpable(context, this);
    }

    private static JavaSites.ConditionVariableSites sites(ThreadContext context) {
        return context.sites.ConditionVariable;
    }

    @Deprecated(since = "9.2.8.0")
    public IRubyObject wait_ruby(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return wait_ruby(context, args[0]);
            case 2:
                return wait_ruby(context, args[0], args[1]);
            default:
                Arity.raiseArgumentError(context, args.length, 1, 2);
                return null; // not reached
        }
    }

    private final ConcurrentLinkedQueue<RubyThread> waiters = new ConcurrentLinkedQueue<>();
    
}
