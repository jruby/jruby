/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.Pointer;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.RubyContext;

import java.nio.charset.StandardCharsets;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class WriteProgramNameNode extends RubyNode {

    /*
     * When we call _NSGetArgv we seem to always get a string that looks like what we'd expect from running ps, but
     * with a null character inserted early. I don't know where this comes from, but it means I don't know how to get
     * the length of space available for writing in the new program name. We therefore about 40 characters, which is
     * a number without any foundation, but it at leaast allows the specs to pass, the functionality to be useful,
     * and probably avoid crashing anyone's programs. I can't pretend this is great engineering.
     */
    private static final int MAX_PROGRAM_NAME_LENGTH = 40;

    public WriteProgramNameNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyString(value)")
    protected Object writeProgramName(DynamicObject value) {
        if (getContext().getCrtExterns() != null) {
            final String valueString = value.toString();
            final Pointer programNameAddress = getContext().getCrtExterns()._NSGetArgv().getPointer(0).getPointer(0);
            programNameAddress.putString(0, valueString, MAX_PROGRAM_NAME_LENGTH, StandardCharsets.UTF_8);
        }

        return value;
    }

}
