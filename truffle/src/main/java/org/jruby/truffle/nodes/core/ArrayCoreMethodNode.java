/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;

@ImportGuards(ArrayGuards.class)
public abstract class ArrayCoreMethodNode extends CoreMethodNode {

    public ArrayCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public ArrayCoreMethodNode(ArrayCoreMethodNode prev) {
        super(prev);
    }

}
