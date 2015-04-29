/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.nodes.Node;
import org.jcodings.transcode.EConv;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

public class RubyEncodingConverter extends RubyBasicObject {

    public EConv getEConv() {
        return econv;
    }

    private EConv econv;

    public RubyEncodingConverter(RubyClass rubyClass, EConv econv) {
        super(rubyClass);
        this.econv = econv;
    }

    public void setEConv(EConv econv) {
        this.econv = econv;
    }

    public static class EncodingConverterAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyEncodingConverter(rubyClass, null);
        }

    }

}
