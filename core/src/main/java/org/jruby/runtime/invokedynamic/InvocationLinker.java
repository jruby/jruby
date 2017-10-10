/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2011 The JRuby Community (and contribs)
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
package org.jruby.runtime.invokedynamic;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.Framing;
import org.jruby.internal.runtime.methods.Scoping;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;

import java.lang.invoke.MethodHandle;

import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static org.jruby.runtime.Helpers.arrayOf;

/**
 * Bootstrapping logic for invokedynamic-based invocation.
 */
public class InvocationLinker {

    public static MethodHandle wrapWithFraming(Signature signature, CallConfiguration callConfig, RubyModule implClass, String name, MethodHandle nativeTarget, StaticScope scope) {
        MethodHandle framePre = getFramePre(signature, callConfig, implClass, name, scope);

        if (framePre != null) {
            MethodHandle framePost = getFramePost(signature, callConfig);

            // break, return, redo handling
            boolean heapScoped = callConfig.scoping() != Scoping.None;
            boolean framed = callConfig.framing() != Framing.None;

            // post logic for frame
            nativeTarget = Binder
                    .from(nativeTarget.type())
                    .tryFinally(framePost)
                    .invoke(nativeTarget);

            // pre logic for frame
            nativeTarget = foldArguments(nativeTarget, framePre);


            // call polling and call number increment
            nativeTarget = Binder
                    .from(nativeTarget.type())
                    .fold(Binder
                            .from(nativeTarget.type().changeReturnType(void.class))
                            .permute(0)
                            .invokeStaticQuiet(lookup(), ThreadContext.class, "callThreadPoll"))
                    .invoke(nativeTarget);
        }

        return nativeTarget;
    }

    public static MethodHandle wrapWithFrameOnly(Signature signature, RubyModule implClass, String name, MethodHandle nativeTarget) {
        MethodHandle framePre = getFrameOnlyPre(signature, CallConfiguration.FrameFullScopeNone, implClass, name);

        MethodHandle framePost = getFramePost(signature, CallConfiguration.FrameFullScopeNone);

        // post logic for frame
        nativeTarget = Binder
                .from(nativeTarget.type())
                .tryFinally(framePost)
                .invoke(nativeTarget);

        // pre logic for frame
        nativeTarget = foldArguments(nativeTarget, framePre);


        // call polling and call number increment
        nativeTarget = Binder
                .from(nativeTarget.type())
                .fold(Binder
                        .from(nativeTarget.type().changeReturnType(void.class))
                        .permute(0)
                        .invokeStaticQuiet(lookup(), ThreadContext.class, "callThreadPoll"))
                .invoke(nativeTarget);

        return nativeTarget;
    }

    public static MethodHandle getFramePre(Signature signature, CallConfiguration callConfig, RubyModule implClass, String name, StaticScope scope) {
        Signature inbound = signature.asFold(void.class);
        SmartBinder binder = SmartBinder
                           .from(inbound);

        switch (callConfig) {
            case FrameFullScopeFull:
                // before logic
                return binder
                        .permute("context", "self", "block")
                        .insert(1, arrayOf("selfClass", "name"), arrayOf(RubyModule.class, String.class), implClass, name)
                        .insert(5, arrayOf("scope"), arrayOf(StaticScope.class), scope)
                        .invokeVirtualQuiet(lookup(), "preMethodFrameAndScope")
                        .handle();

            case FrameFullScopeDummy:
                // before logic
                return binder
                        .permute("context", "self", "block")
                        .insert(1, arrayOf("selfClass", "name"), arrayOf(RubyModule.class, String.class), implClass, name)
                        .insert(5, arrayOf("scope"), arrayOf(StaticScope.class), scope)
                        .invokeVirtualQuiet(lookup(), "preMethodFrameAndDummyScope")
                        .handle();

            case FrameFullScopeNone:
                // before logic
                return binder
                        .permute("context", "self", "block")
                        .insert(1, arrayOf("selfClass", "name"), arrayOf(RubyModule.class, String.class), implClass, name)
                        .invokeVirtualQuiet(lookup(), "preMethodFrameOnly")
                        .handle();

            case FrameNoneScopeDummy:
                // before logic
                return binder
                        .permute("context")
                        .insert(1, arrayOf("selfClass", "scope"), arrayOf(RubyModule.class, StaticScope.class), implClass, scope)
                        .invokeVirtualQuiet(lookup(), "preMethodNoFrameAndDummyScope")
                        .handle();

            case FrameNoneScopeFull:
                return getFrameOnlyPre(signature, callConfig, implClass, name);

        }
        
        return null;
    }

    public static MethodHandle getFrameOnlyPre(Signature signature, CallConfiguration callConfig, RubyModule implClass, String name) {
        Signature inbound = signature.asFold(void.class);
        SmartBinder binder = SmartBinder
                .from(inbound);

        switch (callConfig) {
            case FrameFullScopeNone:
                // before logic
                return binder
                        .permute("context", "self", "block")
                        .insert(1, arrayOf("selfClass", "name"), arrayOf(RubyModule.class, String.class), implClass, name)
                        .invokeVirtualQuiet(lookup(), "preMethodFrameOnly")
                        .handle();

            default:
                throw new RuntimeException("invalid input: " + callConfig);

        }
    }

    public static MethodHandle getFramePost(Signature signature, CallConfiguration callConfig) {
        Signature inbound = signature.asFold(void.class);
        SmartBinder binder = SmartBinder
                               .from(inbound)
                               .permute("context");
        
        switch (callConfig) {
            case FrameFullScopeFull:
                // finally logic
                return binder
                        .invokeVirtualQuiet(lookup(), "postMethodFrameAndScope")
                        .handle();

            case FrameFullScopeDummy:
                // finally logic
                return binder
                        .invokeVirtualQuiet(lookup(), "postMethodFrameAndScope")
                        .handle();

            case FrameFullScopeNone:
                // finally logic
                return binder
                        .invokeVirtualQuiet(lookup(), "postMethodFrameOnly")
                        .handle();

            case FrameNoneScopeFull:
                // finally logic
                return binder
                        .invokeVirtualQuiet(lookup(), "postMethodScopeOnly")
                        .handle();

            case FrameNoneScopeDummy:
                // finally logic
                return binder
                        .invokeVirtualQuiet(lookup(), "postMethodScopeOnly")
                        .handle();

        }

        return null;
    }
}
