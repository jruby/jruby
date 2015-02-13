package org.jruby.truffle.nodes.methods.arguments;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class OptionalKeywordArgMissingNode extends RubyNode {

    private static OptionalKeywordArgMissing instance = new OptionalKeywordArgMissing();

    public OptionalKeywordArgMissingNode(RubyContext context,
            SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static class OptionalKeywordArgMissing {}

    @Override
    public Object execute(VirtualFrame frame) {
        return instance;
    }
    
}
