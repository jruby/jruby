package org.jruby.java.invokers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.function.Supplier;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;

public final class ConstructorInvoker extends RubyToJavaInvoker {
    public ConstructorInvoker(RubyModule host, Supplier<Constructor[]> ctors, String name) {
        super(host, () -> setAccessible(ctors.get()), name);
    }

    @Override
    protected JavaCallable createCallable(Ruby ruby, Member member) {
        return JavaConstructor.wrap((Constructor) member);
    }

    @Override
    protected JavaCallable[] createCallableArray(JavaCallable callable) {
        return new JavaConstructor[] {(JavaConstructor)callable};
    }

    @Override
    protected JavaCallable[] createCallableArray(int size) {
        return new JavaConstructor[size];
    }

    @Override
    protected JavaCallable[][] createCallableArrayArray(int size) {
        return new JavaConstructor[size][];
    }

    @Override
    protected Class[] getMemberParameterTypes(Member member) {
        return ((Constructor) member).getParameterTypes();
    }

    @Override
    @Deprecated(since = "9.1.6.0")
    protected boolean isMemberVarArgs(Member member) {
        return ((Constructor) member).isVarArgs();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallable(self, name, args, args.length);

        final Object[] convertedArgs = convertArguments(constructor, args);
        setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, convertedArgs));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY);
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityZero(self, name);

        setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0});
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityOne(self, name, arg0);
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);

        setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1});
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityTwo(self, name, arg0, arg1);
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        Object cArg1 = arg1.toJava(paramTypes[1]);

        setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0, cArg1));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2});
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityThree(self, name, arg0, arg1, arg2);
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        Object cArg1 = arg1.toJava(paramTypes[1]);
        Object cArg2 = arg2.toJava(paramTypes[2]);

        setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0, cArg1, cArg2));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            final int len = args.length;

            IRubyObject[] newArgs = ArraySupport.newCopy(args, RubyProc.newProc(context.runtime, block, block.type));
            JavaConstructor constructor = (JavaConstructor) findCallable(self, name, newArgs, len + 1);
            final Class<?>[] paramTypes = constructor.getParameterTypes();

            Object[] convertedArgs = new Object[len + 1];
            for (int i = 0; i <= len; i++) {
                convertedArgs[i] = newArgs[i].toJava(paramTypes[i]);
            }

            setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, convertedArgs));

            return self;
        }
        return call(context, self, clazz, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityOne(self, name, proc);
            final Class<?>[] paramTypes = constructor.getParameterTypes();
            Object cArg0 = proc.toJava(paramTypes[0]);

            setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0));

            return self;
        }
        return call(context, self, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityTwo(self, name, arg0, proc);
            final Class<?>[] paramTypes = constructor.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = proc.toJava(paramTypes[1]);

            setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0, cArg1));

            return self;
        }
        return call(context, self, clazz, name, arg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityThree(self, name, arg0, arg1, proc);
            final Class<?>[] paramTypes = constructor.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = arg1.toJava(paramTypes[1]);
            Object cArg2 = proc.toJava(paramTypes[2]);

            setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0, cArg1, cArg2));

            return self;
        }
        return call(context, self, clazz, name, arg0, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            final Class<?>[] paramTypes = constructor.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = arg1.toJava(paramTypes[1]);
            Object cArg2 = arg2.toJava(paramTypes[2]);
            Object cArg3 = proc.toJava(paramTypes[3]);

            setAndCacheProxyObject(context, clazz, proxy, constructor.newInstanceDirect(context, cArg0, cArg1, cArg2, cArg3));

            return self;
        }
        return call(context, self, clazz, name, arg0, arg1, arg2);
    }

    private static void setAndCacheProxyObject(ThreadContext context, RubyModule clazz, JavaProxy proxy, Object object) {
        proxy.setObject(object);

        if (Java.OBJECT_PROXY_CACHE || clazz.getCacheProxy()) {
            context.runtime.getJavaSupport().getObjectProxyCache().put(object, proxy);
        }
    }
}
