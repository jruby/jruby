package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import jnr.ffi.annotations.Meta;
import org.jruby.runtime.CallArgument;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.StringSupport;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;
import java.util.List;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class MetaCallSite extends MutableCallSite {
    private final MethodHandles.Lookup lookup;
    private final String name;
    private final CallArgument[] callArguments;
    private final int flags;
    private final String file;
    private final int line;


    public MetaCallSite(MethodHandles.Lookup lookup, String name, MethodType type, CallArgument[] callArguments, int flags, String file, int line) {
        super(type);
        this.lookup = lookup;
        this.name = name;
        this.callArguments = callArguments;
        this.flags = flags;
        this.file = file;
        this.line = line;

        setTarget(
                Binder.from(type).collect(0, Object[].class).prepend(this).invokeVirtualQuiet("invoke"));
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(MetaCallSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String callArguments, int flags, String file, int line) {
        return new MetaCallSite(lookup, name, type, CallArgument.decodeMulti(callArguments), flags, file, line);
    }

    public IRubyObject invoke(Object[] arguments) {
        System.out.println(Arrays.toString(callArguments));

        return null;
    }
}
