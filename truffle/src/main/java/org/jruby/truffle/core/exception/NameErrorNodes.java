/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.exception;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;

@CoreClass("NameError")
public abstract class NameErrorNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocateNameError(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, nil(), null, null, nil());
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object name(DynamicObject self) {
            return Layouts.NAME_ERROR.getName(self);
        }

    }

    @CoreMethod(names = "receiver")
    public abstract static class ReceiverNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object receiver(DynamicObject self) {
            final Object receiver = Layouts.NAME_ERROR.getReceiver(self);

            // TODO BJF July 21, 2016 Implement name error in message field

            if (receiver == null) {
                throw new RaiseException(coreExceptions().argumentErrorNoReceiver(this));
            }
            return receiver;
        }

    }

    @Primitive(name = "name_error_set_name")
    public abstract static class NameSetNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object setName(DynamicObject error, Object name) {
            Layouts.NAME_ERROR.setName(error, name);
            return name;
        }

    }


}
