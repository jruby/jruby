/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.layouts;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

@Layout
public interface ProcLayout extends BasicObjectLayout {

    DynamicObjectFactory createProcShape(DynamicObject logicalClass,
                                         DynamicObject metaClass);

    DynamicObject createProc(
            DynamicObjectFactory factory,
            @Nullable ProcNodes.Type type,
            @Nullable SharedMethodInfo sharedMethodInfo,
            @Nullable CallTarget callTargetForBlocks,
            @Nullable CallTarget callTargetForProcs,
            @Nullable CallTarget callTargetForLambdas,
            @Nullable MaterializedFrame declarationFrame,
            @Nullable InternalMethod method,
            @Nullable Object self,
            @Nullable DynamicObject block);

    boolean isProc(DynamicObject object);
    boolean isProc(Object object);

    ProcNodes.Type getType(DynamicObject object);

    SharedMethodInfo getSharedMethodInfo(DynamicObject object);
    void setSharedMethodInfo(DynamicObject object, SharedMethodInfo value);

    CallTarget getCallTargetForBlocks(DynamicObject object);
    void setCallTargetForBlocks(DynamicObject object, CallTarget value);

    CallTarget getCallTargetForProcs(DynamicObject object);
    void setCallTargetForProcs(DynamicObject object, CallTarget value);

    CallTarget getCallTargetForLambdas(DynamicObject object);
    void setCallTargetForLambdas(DynamicObject object, CallTarget value);

    MaterializedFrame getDeclarationFrame(DynamicObject object);
    void setDeclarationFrame(DynamicObject object, MaterializedFrame value);

    InternalMethod getMethod(DynamicObject object);
    void setMethod(DynamicObject object, InternalMethod value);

    Object getSelf(DynamicObject object);
    void setSelf(DynamicObject object, Object value);

    DynamicObject getBlock(DynamicObject object);
    void setBlock(DynamicObject object, DynamicObject value);

}
