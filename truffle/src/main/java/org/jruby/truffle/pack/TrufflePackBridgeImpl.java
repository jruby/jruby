/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack;

import com.oracle.truffle.api.CallTarget;
import org.jruby.TrufflePackBridge;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.parser.PackParser;
import org.jruby.truffle.pack.runtime.PackResult;

/**
 * The implementation of the interface from the {@code core} package.
 * <p>
 * Instantiated by {@code core} using reflection, as there is normally no
 * dependency on {@code truffle} from {@code core}.
 */
public class TrufflePackBridgeImpl implements TrufflePackBridge {

    @Override
    public Packer compileFormat(String format) {
        final CallTarget callTarget = new PackParser(null).parse(format.toString(), false);

        return new Packer() {

            @Override
            public PackResult pack(IRubyObject[] objects, int size) {
                return (PackResult) callTarget.call(objects, size);
            }

        };
    }

}
