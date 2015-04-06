package org.jruby.truffle.pack;

import com.oracle.truffle.api.CallTarget;
import org.jruby.TrufflePackBridge;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.parser.PackParser;
import org.jruby.truffle.pack.runtime.PackResult;

public class TrufflePackBridgeImpl implements TrufflePackBridge {

    @Override
    public Packer compileFormat(String format) {
        final CallTarget callTarget = new PackParser(null).parse(format.toString(), false);

        return new Packer() {

            @Override
            public PackResult pack(IRubyObject[] objects, int size) {
                return (PackResult) callTarget.call(objects, size);
            }

        };
    }

}
