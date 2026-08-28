package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
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

public class IsTrueSite extends MutableCallSite {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final MethodType TYPE = methodType(boolean.class, IRubyObject.class);

    public static final MethodHandle INIT_HANDLE = Binder.from(TYPE.insertParameterTypes(0, IsTrueSite.class)).invokeVirtualQuiet(LOOKUP, "init");
    public static final Handle IS_TRUE_BOOTSTRAP_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(IsTrueSite.class),
            "isTrue",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);

    private static final MethodHandle IS_TRUE_HANDLE =
            Binder
                    .from(boolean.class, IRubyObject.class, RubyNil.class, RubyBoolean.False.class)
                    .invokeStaticQuiet(LOOKUP, IsTrueSite.class, "isTruthy");

    public IsTrueSite() {
        super(TYPE);

        setTarget(INIT_HANDLE.bindTo(this));
    }

    public static CallSite isTrue(MethodHandles.Lookup lookup, String name, MethodType type) {
        return new IsTrueSite();
    }

    public boolean init(IRubyObject obj) {
        Ruby runtime = obj.getRuntime();

        IRubyObject nil = runtime.getNil();
        IRubyObject fals = runtime.getFalse();

        setTarget(insertArguments(IS_TRUE_HANDLE, 1, nil, fals));

        return nil != obj && fals != obj;
    }

    public static boolean isTruthy(IRubyObject obj, RubyNil nil, RubyBoolean.False fals) {
        return nil != obj && fals != obj;
    }
}
