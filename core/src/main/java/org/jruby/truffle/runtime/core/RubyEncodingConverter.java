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

import org.jcodings.transcode.EConv;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public class RubyEncodingConverter extends RubyObject {

    public EConv getEConv() {
        return econv;
    }

    public static class RubyEncodingConverterClass extends RubyClass {

        public RubyEncodingConverterClass(RubyContext context, RubyClass encodingClass, RubyClass objectClass) {
            super(context, encodingClass, objectClass, "Converter");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyEncodingConverter(this, null);
        }

    }

    private EConv econv;

    public RubyEncodingConverter(RubyClass rubyClass, EConv econv) {
        super(rubyClass);
        this.econv = econv;
    }

    public void setEConv(EConv econv) {
        this.econv = econv;
    }

}
