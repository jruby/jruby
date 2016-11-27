/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class RubyCallNodeParameters {

    private final RubyContext context;
    private final SourceSection section;
    private final RubyNode receiver;
    private final String methodName;
    private final RubyNode block;
    private final RubyNode[] arguments;
    private final boolean isSplatted;
    private final boolean ignoreVisibility;
    private final boolean isVCall;
    private final boolean isSafeNavigation;
    private final boolean isAttrAssign;

    public RubyCallNodeParameters(RubyContext context, SourceSection section,
            RubyNode receiver, String methodName, RubyNode block, RubyNode[] arguments,
            boolean isSplatted, boolean ignoreVisibility) {
        this(context, section, receiver, methodName, block, arguments, isSplatted, ignoreVisibility, false, false, false);
    }

    public RubyCallNodeParameters(RubyContext context, SourceSection section,
            RubyNode receiver, String methodName, RubyNode block, RubyNode[] arguments,
            boolean isSplatted, boolean ignoreVisibility,
            boolean isVCall, boolean isSafeNavigation, boolean isAttrAssign) {
        this.context = context;
        this.section = section;
        this.receiver = receiver;
        this.methodName = methodName;
        this.block = block;
        this.arguments = arguments;
        this.isSplatted = isSplatted;
        this.ignoreVisibility = ignoreVisibility;
        this.isVCall = isVCall;
        this.isSafeNavigation = isSafeNavigation;
        this.isAttrAssign = isAttrAssign;
    }

    public RubyCallNodeParameters withReceiverAndArguments(RubyNode receiver, RubyNode[] arguments, RubyNode block) {
        return new RubyCallNodeParameters(context, section, receiver, methodName, block, arguments, isSplatted, ignoreVisibility, isVCall, isSafeNavigation, isAttrAssign);
    }

    public RubyContext getContext() {
        return context;
    }

    public SourceSection getSection() {
        return section;
    }

    public RubyNode getReceiver() {
        return receiver;
    }

    public String getMethodName() {
        return methodName;
    }

    public RubyNode getBlock() {
        return block;
    }

    public RubyNode[] getArguments() {
        return arguments;
    }

    public boolean isSplatted() {
        return isSplatted;
    }

    public boolean isIgnoreVisibility() {
        return ignoreVisibility;
    }

    public boolean isVCall() {
        return isVCall;
    }

    public boolean isSafeNavigation() {
        return isSafeNavigation;
    }

    public boolean isAttrAssign() {
        return isAttrAssign;
    }
}
