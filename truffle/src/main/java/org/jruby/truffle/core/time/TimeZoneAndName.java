/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.time;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;

import java.time.ZoneId;

public class TimeZoneAndName {

    private final ZoneId zone;
    private final String name;

    public TimeZoneAndName(ZoneId zone, String name) {
        this.zone = zone;
        this.name = name;
    }

    public ZoneId getZone() {
        return zone;
    }

    public String getName() {
        return name;
    }

    public DynamicObject getNameAsRubyObject(RubyContext context) {
        if (name == null) {
            return context.getCoreLibrary().getNilObject();
        } else {
            return StringOperations.createString(context, context.getRopeTable().getRope(name));
        }
    }
}
