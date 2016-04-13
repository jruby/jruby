/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyGuards;

import java.util.HashMap;
import java.util.Map;

public class InteropManager {

    private final RubyContext context;

    private final Map<String, TruffleObject> exported = new HashMap<>();

    public InteropManager(RubyContext context) {
        this.context = context;
    }

    public void exportObject(DynamicObject name, TruffleObject object) {
        assert RubyGuards.isRubyString(name);
        exported.put(name.toString(), object);
    }

    public Object findExportedObject(String name) {
        return exported.get(name);
    }

    public Object importObject(DynamicObject name) {
        assert RubyGuards.isRubyString(name);
        return context.getEnv().importSymbol(name.toString());
    }

}
