/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.truffle.nodes.*;

public class InlineHeuristic {

    public static boolean shouldInline(InlinableMethodImplementation method) {
        if (method.alwaysInline()) {
            return true;
        }

        return false;
    }

    public static boolean shouldInlineYield(@SuppressWarnings("unused") InlinableMethodImplementation method) {
        return true;
    }

    public static boolean shouldInlineTrace(InlinableMethodImplementation method) {
        return NodeUtil.countNodes(method.getPristineRootNode()) < 100;
    }

}
