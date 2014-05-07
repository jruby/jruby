/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby;

import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.TruffleMethod;
import org.jruby.truffle.translator.TranslatorDriver;

public interface TruffleBridge {
    void init();

    TruffleMethod truffelize(DynamicMethod originalMethod, ArgsNode argsNode, Node bodyNode);

    Object execute(TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode);

    IRubyObject toJRuby(Object object);

    Object toTruffle(IRubyObject object);

    void shutdown();
}
