/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects.instancevariables;

import com.oracle.truffle.api.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.objects.*;

public abstract class ReadSpecializedInstanceVariableNode extends ReadInstanceVariableNode {

    protected final ObjectLayout objectLayout;

    public ReadSpecializedInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, ObjectLayout objectLayout) {
        super(context, sourceSection, name, receiver);
        this.objectLayout = objectLayout;
    }

    protected void respecialize() {
    }

}
