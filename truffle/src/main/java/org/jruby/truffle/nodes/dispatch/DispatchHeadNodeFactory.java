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

    public static CallDispatchHeadNode createMethodCall(RubyContext context) {
        return new CallDispatchHeadNode(
                context,
                false,
                false,
                MissingBehavior.CALL_METHOD_MISSING,
                null);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility) {
        return new CallDispatchHeadNode(
                context,
                ignoreVisibility,
                false,
                MissingBehavior.CALL_METHOD_MISSING,
                null);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, MissingBehavior missingBehavior) {
        return new CallDispatchHeadNode(
                context,
                false,
                false,
                missingBehavior,
                null);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility, MissingBehavior missingBehavior) {
        return new CallDispatchHeadNode(
                context,
                ignoreVisibility,
                false,
                missingBehavior,
                null);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior) {
        return new CallDispatchHeadNode(
                context,
                ignoreVisibility,
                indirect,
                missingBehavior,
                null);
    }

    public static CallDispatchHeadNode createMethodCallOnSelf(RubyContext context) {
        return createMethodCall(context, true);
    }

}
