/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyEncodingConverter extends RubyObject {

    public static class RubyEncodingConverterClass extends RubyClass {

        public RubyEncodingConverterClass(RubyClass objectClass) {
            super(null, null, objectClass, "Converter");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyEncodingConverter(this, null, null);
        }

    }

    private Object source;
    private Object destination;

    public RubyEncodingConverter(RubyClass rubyClass, Object source, Object destination) {
        super(rubyClass);
        this.source = source;
        this.destination = destination;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        getContext().getCoreLibrary().box(source).visitObjectGraph(visitor);
        getContext().getCoreLibrary().box(destination).visitObjectGraph(visitor);
    }

}
