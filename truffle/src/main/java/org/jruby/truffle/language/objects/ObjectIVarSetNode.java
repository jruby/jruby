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
import org.jruby.truffle.language.objects.shared.SharedObjects;

@NodeChildren({ @NodeChild("object"), @NodeChild("name"), @NodeChild("value") })
public abstract class ObjectIVarSetNode extends RubyNode {

    private final boolean checkName;

    public ObjectIVarSetNode(boolean checkName) {
        this.checkName = checkName;
    }

    public abstract Object executeIVarSet(DynamicObject object, String name, Object value);

    @Specialization(guards = "name == cachedName", limit = "getCacheLimit()")
    public Object ivarSetCached(DynamicObject object, String name, Object value,
            @Cached("name") String cachedName,
            @Cached("createWriteFieldNode(checkName(cachedName, object))") WriteObjectFieldNode writeObjectFieldNode) {
        writeObjectFieldNode.execute(object, value);
        return value;
    }

    @TruffleBoundary
    @Specialization(contains = "ivarSetCached")
    public Object ivarSetUncached(DynamicObject object, String name, Object value) {
        if (SharedObjects.isShared(object)) {
            SharedObjects.writeBarrier(value);
            synchronized (object) {
                object.define(checkName(name, object), value);
            }
        } else {
            object.define(checkName(name, object), value);
        }
        return value;
    }

    protected String checkName(String name, DynamicObject object) {
        return checkName ? SymbolTable.checkInstanceVariableName(getContext(), name, object, this) : name;
    }

    protected WriteObjectFieldNode createWriteFieldNode(String name) {
        return WriteObjectFieldNodeGen.create(name);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().INSTANCE_VARIABLE_CACHE;
    }

}
