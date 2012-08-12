package org.jruby.ext.ffi;

import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

final class TypeSizeMapper {
    public static int getTypeSize(ThreadContext context, RubySymbol sizeArg) {
        return context.runtime.getFFI().getTypeResolver().findType(context.runtime, sizeArg).size;
    }
}
