package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;

public class PNode extends PackNode {

    @Override
    public Object execute(VirtualFrame frame) {
        /*
         * P and p print the address of a string. Obviously that doesn't work
         * well in Java. We'll print 0x0000000000000000 with the hope that at
         * least it should page fault if anyone tries to read it.
         */
        advanceSourcePosition(frame);
        return (long) 0;
    }

}
