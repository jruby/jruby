/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

/*
 * Methods catch the return exception and use it as the return value. Procs don't catch return, as returns are
 * lexically associated with the enclosing method. However when a proc becomes a method, such as through
 * Module#define_method it then starts to behave as a method and must catch the return. This node allows us to
 * find the correct place to insert that new {@link CatchReturnNode} when converting a proc to a method.
 */
public class CatchReturnPlaceholderNode extends RubyNode {

    @Child private RubyNode body;
    private final long returnID;

    public CatchReturnPlaceholderNode(RubyContext context, SourceSection sourceSection, RubyNode body, long returnID) {
        super(context, sourceSection);
        this.body = body;
        this.returnID = returnID;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    public RubyNode getBody() {
        return body;
    }

    public long getReturnID() {
        return returnID;
    }

}
