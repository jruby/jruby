package org.jruby.truffle.nodes.methods;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class MarkerNode extends RubyNode {

    private static Marker instance = new Marker();

    public MarkerNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return instance;
    }

    public static class Marker {}

}
