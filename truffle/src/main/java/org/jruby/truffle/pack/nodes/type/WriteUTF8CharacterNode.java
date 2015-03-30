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
        writeBytes(frame, encode((char) value));
        return null;
    }

    @CompilerDirectives.TruffleBoundary
    private byte[] encode(char value) {
        // TODO could hard code UTF8 encoding here easily
        return Character.toString(value).getBytes(StandardCharsets.UTF_8);
    }

}
