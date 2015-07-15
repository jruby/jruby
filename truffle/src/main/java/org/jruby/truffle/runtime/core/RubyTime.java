/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.joda.time.DateTime;

@Deprecated
public class RubyTime extends RubyBasicObject {

    public DateTime dateTime;
    public Object offset;

    public RubyTime(RubyClass timeClass, DateTime dateTime, Object offset) {
        super(timeClass);
        this.dateTime = dateTime;
        assert offset != null;
        this.offset = offset;
    }

}
