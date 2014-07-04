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

    @Children protected final RubyNode[] keyValues;

    public HashLiteralNode(SourceSection sourceSection, RubyNode[] keyValues, RubyContext context) {
        super(context, sourceSection);
        assert keyValues.length % 2 == 0;
        this.keyValues = keyValues;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        if (keyValues.length == 0) {
            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null);
        } else if (keyValues.length <= Options.TRUFFLE_HASHES_SMALL.load() * 2) {
            final Object[] storage = new Object[keyValues.length];

            for (int n = 0; n < storage.length; n++) {
                storage[n] = keyValues[n].execute(frame);
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, storage);
        } else {
            final LinkedHashMap<Object, Object> storage = SlowPathLinkedHashMap.allocate();

            for (int n = 0; n < keyValues.length; n += 2) {
                SlowPathLinkedHashMap.put(storage, keyValues[n].execute(frame), keyValues[n + 1].execute(frame));
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, storage);
        }
    }

    @Override
    public Object isDefined(@SuppressWarnings("unused") VirtualFrame frame) {
        return getContext().makeString("expression");
    }

}
