/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.tracepoint;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.yield.YieldNode;

class TracePointEventNode extends ExecutionEventNode {

    private final ConditionProfile inTraceFuncProfile = ConditionProfile.createBinaryProfile();

    private final RubyContext context;
    private final DynamicObject tracePoint;

    @Child private YieldNode yieldNode;

    @CompilationFinal private DynamicObject path;
    @CompilationFinal private int line;

    public TracePointEventNode(RubyContext context, DynamicObject tracePoint) {
        this.context = context;
        this.tracePoint = tracePoint;
    }

    @Override
    protected void onEnter(VirtualFrame frame) {
        if (inTraceFuncProfile.profile(Layouts.TRACE_POINT.getInsideProc(tracePoint))) {
            return;
        }

        Layouts.TRACE_POINT.setEvent(tracePoint, context.getCoreStrings().LINE.getSymbol());
        Layouts.TRACE_POINT.setPath(tracePoint, getPath());
        Layouts.TRACE_POINT.setLine(tracePoint, getLine());
        Layouts.TRACE_POINT.setBinding(tracePoint, Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), frame.materialize()));

        Layouts.TRACE_POINT.setInsideProc(tracePoint, true);
        
        try {
            getYieldNode().dispatch(frame, Layouts.TRACE_POINT.getProc(tracePoint), tracePoint);
        } finally {
            Layouts.TRACE_POINT.setInsideProc(tracePoint, false);
        }
    }

    private DynamicObject getPath() {
        if (path == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            path = StringOperations.createString(context, context.getRopeTable().getRopeUTF8(getEncapsulatingSourceSection().getSource().getName()));
        }

        return path;
    }

    private int getLine() {
        if (line == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            line = getEncapsulatingSourceSection().getStartLine();
        }

        return line;
    }

    protected YieldNode getYieldNode() {
        if (yieldNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            yieldNode = insert(new YieldNode());
        }

        return yieldNode;
    }

}
