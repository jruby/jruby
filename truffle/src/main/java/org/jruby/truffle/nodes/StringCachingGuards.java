/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes;

import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.util.ByteList;

public abstract class StringCachingGuards {

    public static ByteList privatizeByteList(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.getByteList(string).dup();
    }

    public static boolean byteListsEqual(RubyBasicObject string, ByteList byteList) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.getByteList(string).equal(byteList);
    }

}
