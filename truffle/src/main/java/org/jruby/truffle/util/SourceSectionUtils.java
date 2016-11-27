/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

public abstract class SourceSectionUtils {

    @TruffleBoundary
    public static String fileLine(SourceSection section) {
        Source source = section.getSource();
        if (section.isAvailable()) {
            return String.format("%s:%d", source.getName(), section.getStartLine());
        } else {
            return source.getName();
        }
    }

}
