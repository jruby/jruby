/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

/**
 * Creates a symbol from a string.
 */
@NodeChild("string")
public abstract class StringToSymbolNode extends RubyNode {

    public StringToSymbolNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public StringToSymbolNode(StringToSymbolNode prev) {
        super(prev);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization
    public RubySymbol doString(RubyString string) {
        notDesignedForCompilation("e0c932f09d02468a84500753baf885e8");

        return getContext().newSymbol(string.toString());
    }

}
