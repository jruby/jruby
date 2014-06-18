/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.util.SlowPathLinkedHashMap;
import org.jruby.util.cli.Options;

import java.util.LinkedHashMap;

@NodeInfo(shortName = "hash")
public class HashLiteralNode extends RubyNode {

    @Children protected final RubyNode[] keys;
    @Children protected final RubyNode[] values;

    public HashLiteralNode(SourceSection sourceSection, RubyNode[] keys, RubyNode[] values, RubyContext context) {
        super(context, sourceSection);
        assert keys.length == values.length;
        this.keys = keys;
        this.values = values;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        if (keys.length == 0) {
            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null);
        } else if (keys.length <= Options.TRUFFLE_HASHES_SMALL.load()) {
            final Object[] storage = new Object[keys.length * 2];

            for (int n = 0; n < keys.length; n++) {
                storage[n * 2] = keys[n].execute(frame);
                storage[n * 2 + 1] = values[n].execute(frame);
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, storage);
        } else {
            final LinkedHashMap<Object, Object> storage = SlowPathLinkedHashMap.allocate();

            for (int n = 0; n < keys.length; n++) {
                SlowPathLinkedHashMap.put(storage, keys[n].execute(frame), values[n].execute(frame));
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, storage);
        }
    }

    @Override
    public Object isDefined(@SuppressWarnings("unused") VirtualFrame frame) {
        return getContext().makeString("expression");
    }

}
