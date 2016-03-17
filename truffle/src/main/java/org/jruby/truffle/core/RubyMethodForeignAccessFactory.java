/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.basicobject.BasicObjectForeignAccessFactory;
import org.jruby.truffle.interop.InteropNode;
import org.jruby.truffle.interop.RubyInteropRootNode;

public class RubyMethodForeignAccessFactory extends BasicObjectForeignAccessFactory {

    public RubyMethodForeignAccessFactory(RubyContext context) {
        super(context);
    }

    @Override
    public CallTarget accessRead() {
        return null;
    }

    @Override
    public CallTarget accessWrite() {
        return null;
    }

    @Override
    public CallTarget accessExecute(int i) {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createExecute(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessInvoke(int i) {
        return null;
    }

}
