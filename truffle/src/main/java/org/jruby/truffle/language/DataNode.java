/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.string.StringOperations;

public class DataNode extends RubyNode {

    @Child private SnippetNode snippetNode;

    private final int endPosition;

    public DataNode(int endPosition) {
        this.endPosition = endPosition;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (snippetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            snippetNode = insert(new SnippetNode());
        }

        final String path = getEncapsulatingSourceSection().getSource().getPath();
        final Object data = snippetNode.execute(frame,
                "Truffle.get_data(file, offset)",
                "file", StringOperations.createString(getContext(),
                                StringOperations.encodeRope(path, getContext().getEncodingManager().getLocaleEncoding())),
                "offset", endPosition);

        Layouts.MODULE.getFields(coreLibrary().getObjectClass()).setConstant(getContext(), null, "DATA", data);

        return nil();
    }

}
