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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayLayoutImpl;
import org.jruby.truffle.core.hash.HashLayoutImpl;
import org.jruby.truffle.core.string.StringLayoutImpl;
import org.jruby.truffle.language.RubyNode;

public class InteropHasSize extends RubyNode {

    public InteropHasSize(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final ObjectType type = ((DynamicObject) ForeignAccess.getReceiver(frame)).getShape().getObjectType();

        return type instanceof ArrayLayoutImpl.ArrayType
                || type instanceof HashLayoutImpl.HashType
                || type instanceof StringLayoutImpl.StringType;
    }
}
