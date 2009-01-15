package org.jruby.ext.ffi;

import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class TypeSizeMapper {

    private static final int[] sizes;
    static {
        NativeType[] types = NativeType.values();
        int[] sz = new int[types.length];
        for (int i = 0; i < sz.length; ++i) {
            switch (types[i]) {
                case INT8:
                case UINT8:
                    sz[i] = 1;
                    break;
                case INT16:
                case UINT16:
                    sz[i] = 2;
                    break;
                case INT32:
                case UINT32:
                case FLOAT32:
                    sz[i] = 4;
                    break;
                case INT64:
                case UINT64:
                case FLOAT64:
                    sz[i] = 8;
                    break;
                case LONG:
                case ULONG:
                    sz[i] = Platform.getPlatform().longSize() >> 3;
                    break;
                case POINTER:
                    sz[i] = Platform.getPlatform().addressSize() >> 3;
                    break;
                default:
                    sz[i] = 0;
                    break;
            }
        }
        sizes = sz;
    }

    private static final int getTypeSize(IRubyObject type) {
        int index = (int) RubyNumeric.num2long(type);
        return index >= 0 && index < sizes.length ? sizes[index] : -1;
    }
    private static final int callTypeSize(ThreadContext context, RubyModule ffi, IRubyObject sizeArg) {
        return (int) RubyFixnum.num2long(ffi.callMethod(context, "type_size", sizeArg));
    }
    public static final int getTypeSize(ThreadContext context, IRubyObject sizeArg) {
        final RubyModule ffi = FFIProvider.getModule(context.getRuntime());
        final IRubyObject typeDefs = ffi.fastFetchConstant("TypeDefs");
        final IRubyObject type = ((RubyHash) typeDefs).fastARef(sizeArg);
        final int size = type != null  ? getTypeSize(type) : 0;
        return size > 0 ? size : callTypeSize(context, ffi, sizeArg);
    }
}
