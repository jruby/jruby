package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import jnr.ffi.annotations.Meta;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.api.Create;
import org.jruby.ir.targets.simple.NormalInvokeSite;
import org.jruby.runtime.CallArgument;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.StringSupport;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;
import java.util.List;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
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

    public IRubyObject invoke(Object[] arguments) throws Throwable {
        var binder = Binder.from(type());

        int rubyArgsStart = 0;
        ThreadContext context = null;
        if (callArguments[rubyArgsStart].type() == CallArgument.Type.CONTEXT) {
            context = (ThreadContext) arguments[rubyArgsStart];
            rubyArgsStart++;
        }
        if (callArguments[rubyArgsStart].type() == CallArgument.Type.RECEIVER) rubyArgsStart++;

        int rubyKwargsStart = rubyArgsStart;
        int rubyKwargsCount = callArguments.length - rubyKwargsStart;
        int closureInt = 0;
        CallArgument lastArgument = callArguments[callArguments.length - 1];
        if (lastArgument.type() == CallArgument.Type.BLOCK) {
            closureInt = 1;
            rubyKwargsCount--;
        }
        if (lastArgument.type() == CallArgument.Type.BLOCK_PASS) {
            closureInt = 0;
            rubyKwargsCount--;
        }

        Ruby runtime = context.runtime;
        RubySymbol[] keys = Arrays.stream(callArguments).filter(a -> a.type() == CallArgument.Type.KEYWORD).map(a -> runtime.newSymbol(a.identifier().id())).toArray(RubySymbol[]::new);
        var hashBuilder = Binder
                .from(IRubyObject.class, params(IRubyObject.class, rubyKwargsCount))
                .collect(0, IRubyObject[].class)
                .prepend(runtime, keys)
                .invokeStaticQuiet(MetaCallSite.class, "buildHash");

        binder = binder.collect(rubyKwargsStart, rubyKwargsCount, IRubyObject.class, hashBuilder);

        CallSite specificSite = name.startsWith("invoke") ?
                NormalInvokeSite.bootstrap(lookup, name, binder.type(), closureInt, flags, file, line) :
                SelfInvokeSite.bootstrap(lookup, name, binder.type(), closureInt, flags, file, line);

        var target = binder.invoke(specificSite.dynamicInvoker());

        setTarget(target);

        return (IRubyObject) target.invokeWithArguments(arguments);
    }

    public static IRubyObject buildHash(Ruby runtime, RubySymbol[] keys, IRubyObject[] values) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        for (int i = 0; i < values.length; i++) {
            hash.fastASetSmall(keys[i], values[i]);
        }
        return hash;
    }
}
