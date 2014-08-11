package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;

/**
 * Created by mg on 8/7/14.
 */
public abstract class NewCachedDispatchNode extends NewDispatchNode {

    @Child protected NewDispatchNode next;

    public NewCachedDispatchNode(RubyContext context, NewDispatchNode next) {
        super(context);
        this.next = next;
    }

    public NewCachedDispatchNode(NewCachedDispatchNode prev) {
        super(prev.getContext());
    }

    protected static final boolean isPrimitive(Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return !(receiverObject instanceof  RubyBasicObject);
    }

}
