package org.jruby.truffle.language.objects.shared;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

public class PropagateSharingNode extends Node {

    @Child IsSharedNode isSharedNode;
    @Child WriteBarrierNode writeBarrierNode;

    public static PropagateSharingNode create() {
        return new PropagateSharingNode();
    }

    public PropagateSharingNode() {
        isSharedNode = IsSharedNodeGen.create();
        writeBarrierNode = WriteBarrierNodeGen.create(0);
    }

    public void propagate(DynamicObject source, Object value) {
        if (isSharedNode.executeIsShared(source)) {
            writeBarrierNode.executeWriteBarrier(value);
        }
    }

}
