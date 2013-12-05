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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is the abstract superclass for all call sites in the system.
 */
public abstract class CallSite {
    /** The method name this site calls and caches */
    public final String methodName;
    /** The type of call this site makes */
    protected final CallType callType;

    /**
     * Construct a new CallSite with the given method name and call type.
     *
     * @param methodName the method name this site will call and cache
     * @param callType the type of call to perform (normal, functional, etc)
     * @see org.jruby.runtime.CallType
     */
    public CallSite(String methodName, CallType callType) {
        this.methodName = methodName;
        this.callType = callType;
    }

    // binary typed calls
    /**
     * Call the site's method against the target object, passing a literal long
     * value.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param fixnum the literal long value to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum);
    
    /**
     * Call the site's method against the target object, passing a literal double
     * value.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param flote the literal double value to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote);
    
    // no block
    /**
     * Call the site's method against the target object passing no args.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self);

    /**
     * Call the site's method against the target object passing one argument.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0);

    /**
     * Call the site's method against the target object passing two arguments.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the first argument to pass
     * @param arg1 the second argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1);

    /**
     * Call the site's method against the target object passing two arguments.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the first argument to pass
     * @param arg1 the second argument to pass
     * @param arg2 the third argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);

    /**
     * Call the site's method against the target object passing arguments.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param args the arguments to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args);

    /**
     * Call the site's method against the target object passing arguments.
     * 
     * As a "varargs" method, this will use the length of the args array to
     * dispatch to the correct arity call, rather than dispatching unconditionally
     * to the IRubyObject[] path.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param args the arguments to pass
     * @return the result of the call
     */
    public abstract IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args);

    // with block pass
    /**
     * Call the site's method against the target object passing no arguments and
     * a non-literal (block pass, &) block.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param block the block argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block);

    /**
     * Call the site's method against the target object passing one argument and
     * a non-literal (block pass, &) block.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the argument to pass
     * @param block the block argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block);

    /**
     * Call the site's method against the target object passing two arguments and
     * a non-literal (block pass, &) block.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the first argument to pass
     * @param arg1 the second argument to pass
     * @param block the block argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block);

    /**
     * Call the site's method against the target object passing three arguments and
     * a non-literal (block pass, &) block.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the first argument to pass
     * @param arg1 the second argument to pass
     * @param arg2 the third argument to pass
     * @param block the block argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg3, Block block);

    /**
     * Call the site's method against the target object passing one argument and
     * a non-literal (block pass, &) block.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param args the arguments to pass
     * @param block the block argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block);

    /**
     * Call the site's method against the target object passing one argument and
     * a non-literal (block pass, &) block.
     * 
     * As a "varargs" method, this will use the length of the args array to
     * dispatch to the correct arity call, rather than dispatching unconditionally
     * to the IRubyObject[] path.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param args the arguments to pass
     * @param block the block argument to pass
     * @return the result of the call
     */
    public abstract IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block);

    // with block literal (iter)
    /**
     * Call the site's method against the target object passing no arguments and
     * a literal block. This version handles break jumps by returning their
     * value if this is the appropriate place in the call stack to do so.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param block the literal block to pass
     * @return the result of the call
     */
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, Block block);

    /**
     * Call the site's method against the target object passing one argument and
     * a literal block. This version handles break jumps by returning their
     * value if this is the appropriate place in the call stack to do so.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the argument to pass
     * @param block the literal block to pass
     * @return the result of the call
     */
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block);

    /**
     * Call the site's method against the target object passing two arguments and
     * a literal block. This version handles break jumps by returning their
     * value if this is the appropriate place in the call stack to do so.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the first argument to pass
     * @param arg1 the second argument to pass
     * @param block the literal block to pass
     * @return the result of the call
     */
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block);

    /**
     * Call the site's method against the target object passing three arguments and
     * a literal block. This version handles break jumps by returning their
     * value if this is the appropriate place in the call stack to do so.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param arg0 the first argument to pass
     * @param arg1 the second argument to pass
     * @param arg2 the third argument to pass
     * @param block the literal block to pass
     * @return the result of the call
     */
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block);

    /**
     * Call the site's method against the target object passing arguments and
     * a literal block. This version handles break jumps by returning their
     * value if this is the appropriate place in the call stack to do so.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param args the arguments to pass
     * @param block the literal block to pass
     * @return the result of the call
     */
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block);

    /**
     * Call the site's method against the target object passing arguments and
     * a literal block. This version handles break jumps by returning their
     * value if this is the appropriate place in the call stack to do so.
     * 
     * As a "varargs" method, this will use the length of the args array to
     * dispatch to the correct arity call, rather than dispatching unconditionally
     * to the IRubyObject[] path.
     *
     * @param context the ThreadContext for the current thread
     * @param caller the caller, for visibility checks
     * @param self the target object to call against
     * @param args the arguments to pass
     * @param block the literal block to pass
     * @return the result of the call
     */
    public abstract IRubyObject callVarargsIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block);
}
