package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;

public class WarnNode extends RubyBaseNode {

    @Child CallDispatchHeadNode warnMethod = CallDispatchHeadNode.createMethodCall();

    public Object execute(VirtualFrame frame, String... arguments) {
        final String warningMessage = concatArgumentsToString(arguments);
        final DynamicObject warningString = createString(warningMessage.getBytes(), UTF8Encoding.INSTANCE);
        return warnMethod.call(frame, getContext().getCoreLibrary().getKernelModule(), "warn", warningString);
    }

    @TruffleBoundary
    private String concatArgumentsToString(String... arguments) {
        return String.join("", arguments);
    }
}
