/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.psych;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.object.dsl.Nullable;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;

@Layout
public interface EmitterLayout extends BasicObjectLayout {

    DynamicObjectFactory createEmitterShape(
            DynamicObject logicalClass,
            DynamicObject metaClass);

    DynamicObject createEmitter(
            DynamicObjectFactory factory,
            @Nullable Emitter emitter,
            @Nullable DumperOptions options,
            @Nullable Object io);

    boolean isEmitter(DynamicObject object);

    Emitter getEmitter(DynamicObject object);
    void setEmitter(DynamicObject object, Emitter value);

    DumperOptions getOptions(DynamicObject object);
    void setOptions(DynamicObject object, DumperOptions value);

    Object getIo(DynamicObject object);
    void setIo(DynamicObject object, Object value);

}
