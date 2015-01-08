/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import org.jruby.truffle.runtime.RubyContext;

public class DispatchHeadNodeFactory {

    public static DispatchHeadNode createMethodCall(RubyContext context) {
        return new DispatchHeadNode(
                context,
                false,
                false,
                MissingBehavior.CALL_METHOD_MISSING,
                null,
                DispatchAction.CALL_METHOD);
    }

    public static DispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility) {
        return new DispatchHeadNode(
                context,
                ignoreVisibility,
                false,
                MissingBehavior.CALL_METHOD_MISSING,
                null,
                DispatchAction.CALL_METHOD);
    }

    public static DispatchHeadNode createMethodCall(RubyContext context, MissingBehavior missingBehavior) {
        return new DispatchHeadNode(
                context,
                false,
                false,
                missingBehavior,
                null,
                DispatchAction.CALL_METHOD);
    }

    public static DispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility, MissingBehavior missingBehavior) {
        return new DispatchHeadNode(
                context,
                ignoreVisibility,
                false,
                missingBehavior,
                null,
                DispatchAction.CALL_METHOD);
    }

    public static DispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior) {
        return new DispatchHeadNode(
                context,
                ignoreVisibility,
                indirect,
                missingBehavior,
                null,
                DispatchAction.CALL_METHOD);
    }

    public static DispatchHeadNode createMethodCallOnSelf(RubyContext context) {
        return new DispatchHeadNode(
                context,
                true,
                false,
                MissingBehavior.CALL_METHOD_MISSING,
                null,
                DispatchAction.CALL_METHOD);
    }

}
