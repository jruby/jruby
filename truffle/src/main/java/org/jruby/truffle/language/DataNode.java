/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.util.ByteList;

public class DataNode extends RubyNode {

    private final int endPosition;

    public DataNode(RubyContext context, SourceSection sourceSection, int endPosition) {
        super(context, sourceSection);
        this.endPosition = endPosition;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object data = ruby(
                "Truffle::Primitive.get_data(file, offset)",
                "file", StringOperations.createString(getContext(),
                        ByteList.create(getEncapsulatingSourceSection().getSource().getPath())),
                "offset", endPosition);

        Layouts.MODULE.getFields(coreLibrary().getObjectClass()).setConstant(getContext(), null, "DATA", data);

        return nil();
    }

}
