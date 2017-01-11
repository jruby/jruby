/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.symbol.SymbolTable;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({ @NodeChild("object"), @NodeChild("name") })
public abstract class ObjectIVarGetNode extends RubyNode {

    private final boolean checkName;

    public ObjectIVarGetNode(boolean checkName) {
        this.checkName = checkName;
    }

    public abstract Object executeIVarGet(DynamicObject object, String name);

    @Specialization(guards = "name == cachedName", limit = "getCacheLimit()")
    public Object ivarGetCached(DynamicObject object, String name,
            @Cached("name") String cachedName,
            @Cached("createReadFieldNode(checkName(cachedName, object))") ReadObjectFieldNode readObjectFieldNode) {
        return readObjectFieldNode.execute(object);
    }

    @TruffleBoundary
    @Specialization(contains = "ivarGetCached")
    public Object ivarGetUncached(DynamicObject object, String name) {
        return ReadObjectFieldNode.read(object, checkName(name, object), nil());
    }

    protected String checkName(String name, DynamicObject object) {
        return checkName ? SymbolTable.checkInstanceVariableName(getContext(), name, object, this) : name;
    }

    protected ReadObjectFieldNode createReadFieldNode(String name) {
        return ReadObjectFieldNodeGen.create(name, nil());
    }

    protected int getCacheLimit() {
        return getContext().getOptions().INSTANCE_VARIABLE_CACHE;
    }

}
