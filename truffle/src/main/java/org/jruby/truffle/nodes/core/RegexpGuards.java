/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.StringSupport;

public class RegexpGuards {

    public static boolean isInitialized(DynamicObject regexp) {
        return Layouts.REGEXP.getRegex(regexp) != null;
    }

    public static boolean isRegexpLiteral(DynamicObject regexp) {
        return Layouts.REGEXP.getOptions(regexp).isLiteral();
    }

    public static boolean isValidEncoding(DynamicObject string) {
        return StringOperations.scanForCodeRange(string) != StringSupport.CR_BROKEN;
    }

}
