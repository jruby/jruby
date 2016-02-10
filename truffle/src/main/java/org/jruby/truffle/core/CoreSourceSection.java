/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.source.SourceSection;

public abstract class CoreSourceSection {

    public static SourceSection createCoreSourceSection(String className, String methodName) {
        return SourceSection.createUnavailable("core", String.format("%s#%s", className, methodName));
    }

    public static boolean isCoreSourceSection(SourceSection sourceSection) {
        return sourceSection.getShortDescription().startsWith("core: ");
    }

}
