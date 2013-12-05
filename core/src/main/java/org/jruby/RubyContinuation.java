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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Placeholder until/if we can support this
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Continuation")
public class RubyContinuation extends RubyObject {
    public static class Continuation extends Error implements Unrescuable {
        public Continuation() {tag = null;}
        public Continuation(IRubyObject tag) {
            this.tag = tag;
        }
        public IRubyObject[] args = IRubyObject.NULL_ARRAY;
        public final IRubyObject tag;
        
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private final Continuation continuation;
    private boolean disabled;
    
    public static void createContinuation(Ruby runtime) {
        RubyClass cContinuation = runtime.defineClass("Continuation",runtime.getObject(),runtime.getObject().getAllocator());

        cContinuation.setClassIndex(ClassIndex.CONTINUATION);
        cContinuation.setReifiedClass(RubyContinuation.class);
        
        cContinuation.defineAnnotatedMethods(RubyContinuation.class);
        cContinuation.getSingletonClass().undefineMethod("new");
        
        runtime.setContinuation(cContinuation);
    }

    public RubyContinuation(Ruby runtime) {
        super(runtime, runtime.getContinuation());
        this.continuation = new Continuation();
    }

    /**
     * A RubyContinuation used for catch/throw, which have a tag associated
     *
     * @param runtime Current JRuby runtime
     * @param tag The tag to use
     */
    public RubyContinuation(Ruby runtime, IRubyObject tag) {
        super(runtime, runtime.getContinuation());
        this.continuation = new Continuation(tag);
    }

    public Continuation getContinuation() {
        return continuation;
    }

    @JRubyMethod(name = {"call", "[]"}, rest = true)
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        if (disabled) {
            RubyKernel.raise(context, context.runtime.getThreadError(),
                    new IRubyObject[]{context.runtime.newString("continuations can not be called from outside their scope")},
                    Block.NULL_BLOCK);
        }
        continuation.args = args;
        throw continuation;
    }

    public IRubyObject enter(ThreadContext context, IRubyObject yielded, Block block) {
        try {
            return block.yield(context, yielded);
        } catch (Continuation c) {
            if (c == continuation) {
                if (continuation.args.length == 0) {
                    return context.runtime.getNil();
                } else if (continuation.args.length == 1) {
                    return continuation.args[0];
                } else {
                    return context.runtime.newArrayNoCopy(continuation.args);
                }
            } else {
                throw c;
            }
        } finally {
            disabled = true;
        }
    }
}// RubyContinuation
