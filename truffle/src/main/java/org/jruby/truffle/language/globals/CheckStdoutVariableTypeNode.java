/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.DoesRespondDispatchHeadNode;

public class CheckStdoutVariableTypeNode extends RubyNode {

    @Child private RubyNode child;

    @Child private DoesRespondDispatchHeadNode respondToWriteNode;

    private final BranchProfile unsuitableTypeProfile = BranchProfile.create();

    public CheckStdoutVariableTypeNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    public Object execute(VirtualFrame frame) {
        final Object childValue = child.execute(frame);

        if (getContext().getOptions().PLATFORM_SAFE_IO && (childValue == nil() || !getRespondToWriteNode().doesRespondTo(frame, "write", childValue))) {
            unsuitableTypeProfile.enter();
            throw new RaiseException(coreLibrary().typeErrorMustHaveWriteMethod(childValue, this));
        }

        return childValue;
    }

    private DoesRespondDispatchHeadNode getRespondToWriteNode() {
        if (respondToWriteNode == null) {
            CompilerDirectives.transferToInterpreter();
            respondToWriteNode = insert(new DoesRespondDispatchHeadNode(getContext(), false));
        }

        return respondToWriteNode;
    }

}
