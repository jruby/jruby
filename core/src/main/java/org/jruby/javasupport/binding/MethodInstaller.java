package org.jruby.javasupport.binding;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
* Created by headius on 2/26/15.
*/
public abstract class MethodInstaller extends NamedInstaller {
    private boolean haveLocalMethod;
    protected List<Method> methods;
    protected List<String> aliases;
    public MethodInstaller(){}
    public MethodInstaller(String name, int type) {
        super(name,type);
    }

    // called only by initializing thread; no synchronization required
    void addMethod(Method method, Class<?> javaClass) {
        if (methods == null) {
            methods = new ArrayList<Method>(4);
        }
        methods.add(method);
        haveLocalMethod |= javaClass == method.getDeclaringClass() ||
                method.getDeclaringClass().isInterface();
    }

    // called only by initializing thread; no synchronization required
    void addAlias(String alias) {
        if (aliases == null) {
            aliases = new ArrayList<String>(4);
        }
        if (!aliases.contains(alias)) {
            aliases.add(alias);
        }
    }

    @Override
    boolean hasLocalMethod () {
        return haveLocalMethod;
    }

    void setLocalMethod(boolean b) {
        haveLocalMethod = b;
    }
}
