/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.regexp;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.string.StringOperations;

public class RegexpGuards {

    public static boolean isInitialized(DynamicObject regexp) {
        return Layouts.REGEXP.getRegex(regexp) != null;
    }

    public static boolean isRegexpLiteral(DynamicObject regexp) {
        return Layouts.REGEXP.getOptions(regexp).isLiteral();
    }

    public static boolean isValidEncoding(DynamicObject string) {
        return StringOperations.codeRange(string) != CodeRange.CR_BROKEN;
    }

}
