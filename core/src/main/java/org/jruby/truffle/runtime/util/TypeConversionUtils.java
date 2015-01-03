/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.util;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.UseMethodMissingException;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class TypeConversionUtils {

    public static long convertToLong(RubyNode currentNode, DispatchHeadNode dispatch, VirtualFrame frame, RubyBasicObject object) {
        try {
            return dispatch.callLongFixnum(frame, object, "to_i", null);
        } catch (UseMethodMissingException e) {
            throw new RaiseException(currentNode.getContext().getCoreLibrary().typeErrorCantConvertInto(object.getLogicalClass().getName(),
                    currentNode.getContext().getCoreLibrary().getIntegerClass().getName(), currentNode));
        }
    }
}
