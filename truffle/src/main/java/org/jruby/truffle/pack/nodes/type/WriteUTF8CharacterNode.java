package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteUTF8CharacterNode extends PackNode {

    @Specialization
    public Object write(VirtualFrame frame, long value) {
        CompilerDirectives.bailout("#pack U doesn't work in the compiler yet");
        // TODO could probably writeBytes this here, or at least pick out some common cases (ASCII)
        writeBytes(frame, Character.toString((char) value).getBytes(StandardCharsets.UTF_8));
        return null;
    }

}
