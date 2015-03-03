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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;

@ImportStatic(HashGuards.class)
public abstract class HashCoreMethodNode extends CoreMethodNode {

    public HashCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public HashCoreMethodNode(HashCoreMethodNode prev) {
        super(prev);
    }

}
