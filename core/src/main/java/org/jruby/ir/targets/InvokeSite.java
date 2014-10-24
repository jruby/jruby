package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.JRubyCallSite;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

/**
* Created by headius on 10/23/14.
*/
public class InvokeSite extends MutableCallSite {
    final Signature signature;
    final Signature fullSignature;
    final int arity;
    protected final String methodName;

    public String name() {
        return methodName;
    }

    public final CallType callType;

    public InvokeSite(MethodType type, String name, CallType callType) {
        super(type);
        this.methodName = name;
        this.callType = callType;

        Signature startSig;
        int argOffset;

        if (callType == CallType.SUPER) {
            // super calls receive current class argument, so offsets and signature are different
            startSig = JRubyCallSite.STANDARD_SUPER_SIG;
            argOffset = 4;
        } else {
            startSig = JRubyCallSite.STANDARD_SITE_SIG;
            argOffset = 3;
        }

        int arity;
        if (type.parameterType(type.parameterCount() - 1) == Block.class) {
            arity = type.parameterCount() - (argOffset + 1);

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            startSig = startSig.appendArg("block", Block.class);
            fullSignature = signature = startSig;
        } else {
            arity = type.parameterCount() - argOffset;

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            signature = startSig;
            fullSignature = startSig.appendArg("block", Block.class);
        }

        this.arity = arity;
    }

    public static CallSite bootstrap(InvokeSite site, MethodHandles.Lookup lookup) {
        MethodHandle handle;

        handle = site.prepareBinder().invokeVirtualQuiet(lookup, "invoke");

        site.setTarget(handle);

        return site;
    }

    public Binder prepareBinder() {
        SmartBinder binder = SmartBinder.from(signature);

        binder = binder.insert(0, "site", this);

        if (arity == -1) {
            // do nothing, already have IRubyObject[] in args
        } else if (arity == 0) {
            binder = binder.insert(4, "args", IRubyObject.NULL_ARRAY);
        } else {
            binder = binder
                    .collect("args", "arg[0-9]+");
        }

        if (signature.lastArgType() != Block.class) {
            binder = binder.append("block", Block.NULL_BLOCK);
        }

        return binder.binder();
    }

    MethodHandle getHandle(RubyClass selfClass, SwitchPoint switchPoint, InvokeSite site, DynamicMethod method) throws Throwable {
        boolean blockGiven = signature.lastArgType() == Block.class;

        MethodHandle mh = Bootstrap.buildNativeHandle(site, method, blockGiven);
        if (mh == null) mh = Bootstrap.buildJittedHandle(site, method, blockGiven);
        if (mh == null) mh = Bootstrap.buildGenericHandle(site, method, selfClass);

        assert mh != null : "we should have a method handle of some sort by now";

        MethodHandle fallback;
        SmartBinder fallbackBinder = SmartBinder
                .from(site.signature);

        // fallbacks only up to arity 3
        if (site.arity > 3) fallbackBinder = fallbackBinder.collect("args", "arg.*");

        // insert site and bind to target method
        fallback = prepareBinder()
                .invokeVirtual(Bootstrap.LOOKUP, "invoke");

        MethodHandle test = SmartBinder
                .from(site.signature.changeReturn(boolean.class))
                .permute("self")
                .insert(0, "selfClass", RubyClass.class, selfClass)
                .invokeStatic(Bootstrap.LOOKUP, Bootstrap.class, "testType").handle();

        mh = MethodHandles.guardWithTest(test, mh, fallback);
        mh = switchPoint.guardWithTest(mh, fallback);

        return mh;
    }
}
