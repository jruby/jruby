package org.jruby.truffle.nodes.rubinius;


import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodNode;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyObject;
import org.jruby.truffle.runtime.rubinius.RubiniusChannel;

@CoreClass(name = "Channel")
public abstract class ChannelNodes {

    @CoreMethod(names = {"send", "<<"}, minArgs = 1, maxArgs = 1)
    public abstract static class SendNode extends CoreMethodNode {
        public SendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SendNode(SendNode prev) {
            super(prev);
        }

        @Specialization
        public Object send(RubiniusChannel self, RubyObject value) {
            self.send(value);
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "receive")
    public abstract static class ReceiveNode extends CoreMethodNode {
        public ReceiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReceiveNode(ReceiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object receive(RubiniusChannel self) {
            return self.receive();
        }
    }

    @CoreMethod(names = "receive_timeout")
    public abstract static class ReceiveTimeoutNode extends CoreMethodNode {

        private static final int NANOSECONDS = 1000000;

        public ReceiveTimeoutNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReceiveTimeoutNode(ReceiveTimeoutNode prev) {
            super(prev);
        }

        @Specialization
        public Object receive_timeout(RubiniusChannel self, int timeout) {
            return self.receive_timeout(timeout * NANOSECONDS);
        }

        @Specialization
        public Object receive_timeout(RubiniusChannel self, long timeout) {
            return self.receive_timeout(timeout * NANOSECONDS);
        }

        @Specialization
        public Object receive_timeout(RubiniusChannel self, double timeout) {
            return self.receive_timeout((long) (timeout * NANOSECONDS));
        }

        @Specialization
        public Object receive_timeout(RubiniusChannel self, RubyNilClass nil) {
            return self.receive_timeout(-1);
        }
    }

    @CoreMethod(names = "try_receive")
    public abstract static class TryReceiveNode extends CoreMethodNode {
        public TryReceiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TryReceiveNode(TryReceiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object try_receive(RubiniusChannel self) {
            return self.try_receive();
        }
    }
}
