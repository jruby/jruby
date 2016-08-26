/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.arguments.RubyArguments;

public class EnumeratorSizeNode extends RubyNode {

    @Child private RubyNode method;
    @Child private SnippetNode snippetNode;
    private final ConditionProfile noBlockProfile = ConditionProfile.createBinaryProfile();
    private final String snippet;

    public EnumeratorSizeNode(String enumeratorSize, String methodName, RubyNode method) {
        this.method = method;
        this.snippet = "to_enum(:" + methodName + ") { " + enumeratorSize + " }";
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject block = RubyArguments.getBlock(frame);

        if (noBlockProfile.profile(block == null)) {
            if (snippetNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                snippetNode = insert(new SnippetNode());
            }
            return snippetNode.execute(frame, snippet);
        } else {
            return method.execute(frame);
        }
    }

}