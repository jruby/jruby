package org.jruby.truffle.runtime.util;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.UseMethodMissingException;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyObject;

public class TypeConversionUtils {

    public static long convertToLong(RubyNode currentNode, DispatchHeadNode dispatch, VirtualFrame frame, RubyObject object) {
        try {
            return dispatch.callLongFixnum(frame, object, "to_i", null);
        } catch (UseMethodMissingException e) {
            throw new RaiseException(currentNode.getContext().getCoreLibrary().typeErrorCantConvertInto(object.getLogicalClass().getName(),
                    currentNode.getContext().getCoreLibrary().getIntegerClass().getName(), currentNode));
        }
    }
}
