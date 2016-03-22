/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.basicobject.BasicObjectForeignAccessFactory;
import org.jruby.truffle.interop.InteropGetSizeProperty;
import org.jruby.truffle.interop.InteropStringIsBoxed;
import org.jruby.truffle.interop.InteropStringUnboxNode;
import org.jruby.truffle.interop.RubyInteropRootNode;
import org.jruby.truffle.interop.UnresolvedInteropStringReadNode;
import org.jruby.truffle.language.literal.BooleanLiteralNode;

public class StringForeignAccessFactory extends BasicObjectForeignAccessFactory {

    public StringForeignAccessFactory(RubyContext context) {
        super(context);
    }

    @Override
    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(new InteropStringIsBoxed(context, null)));
    }

    @Override
    public CallTarget accessGetSize() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(new InteropGetSizeProperty(context, null)));
    }

    @Override
    public CallTarget accessUnbox() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(new InteropStringUnboxNode(context, null)));
    }

    @Override
    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(new UnresolvedInteropStringReadNode(context, null)));
    }

}
