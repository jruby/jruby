package org.jruby.java.invokers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstructorInvoker extends RubyToJavaInvoker {

    public ConstructorInvoker(RubyModule host, List<Constructor> ctors) {
        super(host, ctors.toArray(new Constructor[ctors.size()]));

        trySetAccessible(getAccessibleObjects());
    }

    @Override
    protected JavaCallable createCallable(Ruby ruby, Member member) {
        return JavaConstructor.create(ruby, (Constructor)member);
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
    protected boolean isMemberVarArgs(Member member) {
        return ((Constructor) member).isVarArgs();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        JavaProxy proxy = castJavaProxy(self);

        int len = args.length;
        final Object[] convertedArgs;
        JavaConstructor constructor = (JavaConstructor) findCallable(self, name, args, len);
        if (constructor.isVarArgs()) {
            len = constructor.getParameterTypes().length - 1;
            convertedArgs = new Object[len + 1];
            for (int i = 0; i < len && i < args.length; i++) {
                convertedArgs[i] = convertArg(args[i], constructor, i);
            }
            convertedArgs[len] = convertVarargs(args, constructor);
        } else {
            convertedArgs = new Object[len];
            for (int i = 0; i < len && i < args.length; i++) {
                convertedArgs[i] = convertArg(args[i], constructor, i);
            }
        }

        proxy.setObject(constructor.newInstanceDirect(context, convertedArgs));
        
        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY);
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityZero(self, name);

        proxy.setObject(constructor.newInstanceDirect(context));
        
        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0});
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityOne(self, name, arg0);
        Object cArg0 = convertArg(arg0, constructor, 0);

        proxy.setObject(constructor.newInstanceDirect(context, cArg0));
        
        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1});
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityTwo(self, name, arg0, arg1);
        Object cArg0 = convertArg(arg0, constructor, 0);
        Object cArg1 = convertArg(arg1, constructor, 1);

        proxy.setObject(constructor.newInstanceDirect(context, cArg0, cArg1));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2});
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor) findCallableArityThree(self, name, arg0, arg1, arg2);
        Object cArg0 = convertArg(arg0, constructor, 0);
        Object cArg1 = convertArg(arg1, constructor, 1);
        Object cArg2 = convertArg(arg2, constructor, 2);

        proxy.setObject(constructor.newInstanceDirect(context, cArg0, cArg1, cArg2));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            int len = args.length;
            // too much array creation!
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallable(self, name, intermediate, len + 1);
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = convertArg(intermediate[i], constructor, i);
            }

            proxy.setObject(constructor.newInstanceDirect(context, convertedArgs));

            return self;
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityOne(self, name, proc);
            Object cArg0 = convertArg(proc, constructor, 0);

            proxy.setObject(constructor.newInstanceDirect(context, cArg0));

            return self;
        } else {
            return call(context, self, clazz, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityTwo(self, name, arg0, proc);
            Object cArg0 = convertArg(arg0, constructor, 0);
            Object cArg1 = convertArg(proc, constructor, 1);

            proxy.setObject(constructor.newInstanceDirect(context, cArg0, cArg1));

            return self;
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityThree(self, name, arg0, arg1, proc);
            Object cArg0 = convertArg(arg0, constructor, 0);
            Object cArg1 = convertArg(arg1, constructor, 1);
            Object cArg2 = convertArg(proc, constructor, 2);

            proxy.setObject(constructor.newInstanceDirect(context, cArg0, cArg1, cArg2));

            return self;
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaConstructor constructor = (JavaConstructor) findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            Object cArg0 = convertArg(arg0, constructor, 0);
            Object cArg1 = convertArg(arg1, constructor, 1);
            Object cArg2 = convertArg(arg2, constructor, 2);
            Object cArg3 = convertArg(proc, constructor, 3);

            proxy.setObject(constructor.newInstanceDirect(context, cArg0, cArg1, cArg2, cArg3));

            return self;
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }
}
