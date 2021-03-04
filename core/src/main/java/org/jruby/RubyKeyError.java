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
<<<<<<< HEAD
 * Copyright (C) 2017 Miguel Landaeta <miguel@miguel.cc>
 *
=======
>>>>>>> master
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

package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.KeyError;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author Miguel Landaeta
 */
@JRubyClass(name="KeyError", parent="IndexError")
public class RubyKeyError extends RubyIndexError {
    private static final String[] VALID_KEYS = {"receiver", "key"};
    private IRubyObject receiver;
    private IRubyObject key;

    protected RubyKeyError(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    public RubyKeyError(Ruby runtime, RubyClass exceptionClass, String message, IRubyObject recv, IRubyObject key) {
        super(runtime, exceptionClass, message);
        this.receiver = recv;
        this.key = key;
    }

    static RubyClass define(Ruby runtime, RubyClass superClass) {
        RubyClass KeyError = runtime.defineClass("KeyError", superClass, RubyKeyError::new);
        KeyError.defineAnnotatedMethods(RubyKeyError.class);
        KeyError.setReifiedClass(RubyKeyError.class);
        return KeyError;
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new KeyError(message, this);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject messageOrKwargs) {
        IRubyObject[] receiverKey = ArgsUtil.extractKeywordArgs(context, messageOrKwargs, VALID_KEYS);

        if (receiverKey == null) return initialize(context, messageOrKwargs, null);

        return initializeCommon(context, context.nil, receiverKey);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject message, IRubyObject kwargs) {
        IRubyObject[] receiverKey = ArgsUtil.extractKeywordArgs(context, kwargs, VALID_KEYS);

        return initializeCommon(context, message, receiverKey);
    }

    private IRubyObject initializeCommon(ThreadContext context, IRubyObject message, IRubyObject[] receiverKey) {
        IRubyObject receiver;
        IRubyObject key;
        if (receiverKey == null) {
            receiver = context.nil;
            key = context.nil;
        } else {
            receiver = receiverKey[0];
            key = receiverKey[1];
        }

        setMessage(message);
        this.receiver = receiver;
        this.key = key;

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject message, IRubyObject receiver, IRubyObject key) {
        setMessage(message);
        this.receiver = receiver;
        this.key = key;

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject receiver() {
        return receiver;
    }

    @JRubyMethod
    public IRubyObject key() {
        return key;
    }
}