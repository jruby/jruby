package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;


public class EncodingPseudoVariableNode extends RubyNode {

    public EncodingPseudoVariableNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);

    }

    @Override
    public Object execute(VirtualFrame frame) {
        return getContext().getCoreLibrary().getDefaultEncoding();
    }
}