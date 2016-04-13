/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.RubyObjectType;

@AcceptMessage(value = "IS_NULL", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignIsNullNode extends ForeignIsNullBaseNode {

    @Child private Node findContextNode;
    @CompilationFinal RubyContext context;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object) {
        return object == getContext().getCoreLibrary().getNilObject();
    }

    private RubyContext getContext() {
        if (context == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
        }

        return context;
    }

}
