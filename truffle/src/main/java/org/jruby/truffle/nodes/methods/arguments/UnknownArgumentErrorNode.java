package org.jruby.truffle.nodes.methods.arguments;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class UnknownArgumentErrorNode extends RubyNode {

    private final String label;

    public UnknownArgumentErrorNode(RubyContext context,
            SourceSection sourceSection, String label) {
        super(context, sourceSection);
        this.label = label;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new RaiseException(getContext().getCoreLibrary().argumentError(
                "unknown keyword: " + label, this));
    }

}