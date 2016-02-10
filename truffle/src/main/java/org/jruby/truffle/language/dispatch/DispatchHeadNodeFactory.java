/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import org.jruby.truffle.RubyContext;

public class DispatchHeadNodeFactory {

    public static CallDispatchHeadNode createMethodCall(RubyContext context) {
        return new CallDispatchHeadNode(
                context,
                false,
                MissingBehavior.CALL_METHOD_MISSING);
    }

    public static CallDispatchHeadNode createMethodCallOnSelf(RubyContext context) {
        return createMethodCall(context, true);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility) {
        return new CallDispatchHeadNode(
                context,
                ignoreVisibility,
                MissingBehavior.CALL_METHOD_MISSING);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, MissingBehavior missingBehavior) {
        return new CallDispatchHeadNode(
                context,
                false,
                missingBehavior);
    }

    public static CallDispatchHeadNode createMethodCall(RubyContext context, boolean ignoreVisibility, MissingBehavior missingBehavior) {
        return new CallDispatchHeadNode(
                context,
                ignoreVisibility,
                missingBehavior);
    }

}
