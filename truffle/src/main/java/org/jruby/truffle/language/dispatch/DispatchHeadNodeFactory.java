/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

public class DispatchHeadNodeFactory {

    public static CallDispatchHeadNode createMethodCall() {
        return createMethodCall(false);
    }

    public static CallDispatchHeadNode createMethodCallOnSelf() {
        return createMethodCall(true);
    }

    public static CallDispatchHeadNode createMethodCall(boolean ignoreVisibility) {
        return createMethodCall(
                ignoreVisibility,
                MissingBehavior.CALL_METHOD_MISSING);
    }

    public static CallDispatchHeadNode createMethodCall(MissingBehavior missingBehavior) {
        return createMethodCall(
                false,
                missingBehavior);
    }

    public static CallDispatchHeadNode createMethodCall(boolean ignoreVisibility, MissingBehavior missingBehavior) {
        return new CallDispatchHeadNode(
                ignoreVisibility,
                missingBehavior);
    }

}
