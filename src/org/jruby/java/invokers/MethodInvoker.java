package org.jruby.java.invokers;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaMethod;

public abstract class MethodInvoker extends RubyToJavaInvoker {
    MethodInvoker(RubyModule host, List<Method> methods) {
        super(host, methods.toArray(new Method[methods.size()]));
        trySetAccessible(getAccessibleObjects());
    }

    MethodInvoker(RubyModule host, Method method) {
        super(host, new Method[] {method});
        trySetAccessible(getAccessibleObjects());
    }

    protected JavaCallable createCallable(Ruby ruby, Member member) {
        return JavaMethod.create(ruby, (Method)member);
    }

    protected JavaCallable[] createCallableArray(JavaCallable callable) {
        return new JavaMethod[] {(JavaMethod)callable};
    }

    protected JavaCallable[] createCallableArray(int size) {
        return new JavaMethod[size];
    }

    protected JavaCallable[][] createCallableArrayArray(int size) {
        return new JavaMethod[size][];
    }

    protected Class[] getMemberParameterTypes(Member member) {
        return ((Method)member).getParameterTypes();
    }

    protected boolean isMemberVarArgs(Member member) {
        return ((Method)member).isVarArgs();
    }
}
