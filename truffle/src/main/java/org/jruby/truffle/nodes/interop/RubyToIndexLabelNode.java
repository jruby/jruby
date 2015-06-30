/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.SymbolNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;

public abstract class RubyToIndexLabelNode extends TargetableRubyNode {

    public RubyToIndexLabelNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    public Object doRubyString(RubyString index) {
        return index.toString();
    }

    @Specialization(guards = "isRubySymbol(index)")
    public Object doRubySymbol(RubyBasicObject index) {
        return SymbolNodes.getString(index);
    }

    @Specialization(guards = "!isRubySymbol(index)")
    public Object doObject(Object index) {
        return index;
    }

}
