package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    @CoreMethod(names = "find", isModuleMethod = true, needsSelf = false, maxArgs = 1, minArgs = 1)
    public abstract static class FindNode extends CoreMethodNode {

        public FindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FindNode(FindNode prev) {
            super(prev);
        }

        @Specialization
        public Object find(RubyString name) {
            return RubyEncoding.findEncodingByName(name);
        }

    }
}
