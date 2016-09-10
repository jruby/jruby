package org.jruby.truffle.core;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;

public interface InlinableBuiltin extends NodeInterface {

    public Object executeBuiltin(VirtualFrame frame, Object... arguments);

}
