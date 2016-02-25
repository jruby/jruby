/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.objects.ThreadLocalObjectNode;
import org.jruby.truffle.language.objects.ThreadLocalObjectNodeGen;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;

public class EnsureNode extends RubyNode {

    @Child private RubyNode tryPart;
    @Child private RubyNode ensurePart;
    @Child private ThreadLocalObjectNode threadLocalNode;
    @Child private ReadObjectFieldNode readDollarBang;
    @Child private WriteObjectFieldNode writeDollarBang;

    private final BranchProfile rubyExceptionPath = BranchProfile.create();
    private final BranchProfile javaExceptionPath = BranchProfile.create();

    public EnsureNode(RubyContext context, SourceSection sourceSection, RubyNode tryPart, RubyNode ensurePart) {
        super(context, sourceSection);
        this.tryPart = tryPart;
        this.ensurePart = ensurePart;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value;

        try {
            value = tryPart.execute(frame);
        } catch (RaiseException exception) {
            rubyExceptionPath.enter();
            throw setLastExceptionAndRunEnsure(frame, exception);
        } catch (Throwable throwable) {
            javaExceptionPath.enter();
            ensurePart.executeVoid(frame);
            throw throwable;
        }

        ensurePart.executeVoid(frame);

        return value;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            tryPart.executeVoid(frame);
        } catch (RaiseException exception) {
            rubyExceptionPath.enter();
            throw setLastExceptionAndRunEnsure(frame, exception);
        } catch (Throwable throwable) {
            javaExceptionPath.enter();
            ensurePart.executeVoid(frame);
            throw throwable;
        }

        ensurePart.executeVoid(frame);
    }

    private RaiseException setLastExceptionAndRunEnsure(VirtualFrame frame, RaiseException exception) {
        final DynamicObject threadLocals = getThreadLocalsObject(frame);

        final Object lastException = readDollarBang(threadLocals);
        writeDollarBang(threadLocals, exception.getException());

        try {
            ensurePart.executeVoid(frame);
            return exception;
        } finally {
            writeDollarBang(threadLocals, lastException);
        }
    }

    private DynamicObject getThreadLocalsObject(VirtualFrame frame) {
        if (threadLocalNode == null) {
            CompilerDirectives.transferToInterpreter();
            threadLocalNode = insert(ThreadLocalObjectNodeGen.create(getContext(), getEncapsulatingSourceSection()));
        }

        return threadLocalNode.executeDynamicObject(frame);
    }

    private void writeDollarBang(DynamicObject threadLocals, Object value) {
        if (writeDollarBang == null) {
            CompilerDirectives.transferToInterpreter();
            writeDollarBang = insert(WriteObjectFieldNodeGen.create(getContext(), "$!"));
        }

        writeDollarBang.execute(threadLocals, value);
    }

    private Object readDollarBang(DynamicObject threadLocals) {
        if (readDollarBang == null) {
            CompilerDirectives.transferToInterpreter();
            readDollarBang = insert(ReadObjectFieldNodeGen.create(getContext(), "$!", nil()));
        }

        return readDollarBang.execute(threadLocals);
    }

}
