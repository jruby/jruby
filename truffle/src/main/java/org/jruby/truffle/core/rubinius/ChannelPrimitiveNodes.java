/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;

public abstract class ChannelPrimitiveNodes {

    @Primitive(name = "channel_new")
    public static abstract class ChannelNewPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject channelNew() {
            final DynamicObject channelClass = coreLibrary().getRubiniusChannelClass();
            return Layouts.CLASS.getInstanceFactory(channelClass).newInstance();
        }

    }

}
