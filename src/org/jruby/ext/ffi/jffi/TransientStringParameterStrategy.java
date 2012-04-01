package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.ObjectParameterType;
import org.jruby.RubyString;

/**
 *
 */
public class TransientStringParameterStrategy extends PointerParameterStrategy {
    public TransientStringParameterStrategy() {
        super(HEAP, ObjectParameterType.create(ObjectParameterType.ARRAY, ObjectParameterType.BYTE));
    }

    @Override
    public long address(Object parameter) {
        return 0;
    }

    @Override
    public Object object(Object parameter) {
        return ((RubyString) parameter).getByteList().unsafeBytes();
    }

    @Override
    public int offset(Object parameter) {
        return ((RubyString) parameter).getByteList().begin();
    }

    @Override
    public int length(Object parameter) {
        return ((RubyString) parameter).getByteList().length();
    }
}
