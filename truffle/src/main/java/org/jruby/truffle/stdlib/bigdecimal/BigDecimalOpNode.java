/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild(value = "a", type = RubyNode.class),
        @NodeChild(value = "b", type = RubyNode.class),
})
public abstract class BigDecimalOpNode extends BigDecimalCoreMethodNode {

    @CreateCast("b")
    protected RubyNode castB(RubyNode b) {
        return BigDecimalCoerceNodeGen.create(b);
    }

}
