/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.basicobject.BasicObjectForeignAccessFactory;
import org.jruby.truffle.interop.InteropGetSizeProperty;
import org.jruby.truffle.interop.RubyInteropRootNode;
import org.jruby.truffle.language.literal.BooleanLiteralNode;

public class ArrayForeignAccessFactory extends BasicObjectForeignAccessFactory {

    public ArrayForeignAccessFactory(RubyContext context) {
        super(context);
    }

}
