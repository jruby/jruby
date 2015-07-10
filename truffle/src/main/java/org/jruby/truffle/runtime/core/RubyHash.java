/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.hash.Entry;

@Deprecated
public class RubyHash extends RubyBasicObject {

    public RubyProc defaultBlock;
    public Object defaultValue;
    public Object store;
    public int size;
    public Entry firstInSequence;
    public Entry lastInSequence;
    public boolean compareByIdentity;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int size, Entry firstInSequence, Entry lastInSequence, DynamicObject dynamicObject) {
        super(rubyClass, dynamicObject);
        this.defaultBlock = defaultBlock;
        this.defaultValue = defaultValue;
        this.store = store;
        this.size = size;
        this.firstInSequence = firstInSequence;
        this.lastInSequence = lastInSequence;
        assert HashNodes.verifyStore(this);
    }

}
