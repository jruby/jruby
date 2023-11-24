package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.RubyNil;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class IsNilSite extends MutableCallSite {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final MethodType TYPE = methodType(boolean.class, IRubyObject.class);
    public static final Handle IS_NIL_BOOTSTRAP_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(IsNilSite.class),
            "isNil",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);

    private static final MethodHandle INIT_HANDLE =
            Binder.from(TYPE.insertParameterTypes(0, IsNilSite.class)).invokeVirtualQuiet(LOOKUP, "init");

    private static final MethodHandle IS_NIL_HANDLE =
            Binder
                    .from(boolean.class, IRubyObject.class, RubyNil.class)
                    .invokeStaticQuiet(LOOKUP, IsNilSite.class, "isNil");

    public IsNilSite() {
        super(TYPE);

        setTarget(INIT_HANDLE.bindTo(this));
    }

    public static CallSite isNil(MethodHandles.Lookup lookup, String name, MethodType type) {
        return new IsNilSite();
    }

    public boolean init(IRubyObject obj) {
        IRubyObject nil = obj.getRuntime().getNil();

        setTarget(insertArguments(IS_NIL_HANDLE, 1, nil));

        return nil == obj;
    }

    public static boolean isNil(IRubyObject obj, RubyNil nil) {
        return nil == obj;
    }
}
