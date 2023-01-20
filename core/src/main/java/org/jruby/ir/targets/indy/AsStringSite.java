package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.simple.NormalInvokeSite;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class AsStringSite extends NormalInvokeSite {
    public AsStringSite(MethodType type, String file, int line) {
        super(type, "to_s", false, file, line);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(AsStringSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String file, int line) {
        InvokeSite site = new AsStringSite(type, file, line);

        return InvokeSite.bootstrap(site, lookup);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);

        NormalInvokeSite toS = new NormalInvokeSite(type(), "to_s", false, file, line);
        MethodHandle toS_handle = toS.dynamicInvoker();

        MethodHandle checkcast = Binder.from(type().changeReturnType(boolean.class))
                .permute(2)
                .invokeStaticQuiet(LOOKUP, AsStringSite.class, "isString");

        MethodHandle guardedToS = MethodHandles.guardWithTest(checkcast, toS_handle, Binder.from(type()).permute(2).identity());

        setTarget(guardedToS);

        return self.asString();
    }

    public static boolean isString(IRubyObject maybeString) {
        return maybeString instanceof RubyString;
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
}