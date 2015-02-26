package org.jruby.javasupport.binding;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
* Created by headius on 2/26/15.
*/
public abstract class MethodInstaller extends NamedInstaller {

    protected final List<Method> methods = new ArrayList<Method>(4);
    protected List<String> aliases;
    private boolean localMethod;

    public MethodInstaller(String name, int type) { super(name, type); }

    // called only by initializing thread; no synchronization required
    void addMethod(final Method method, final Class<?> clazz) {
        this.methods.add(method);
        localMethod |=
            clazz == method.getDeclaringClass() ||
            method.getDeclaringClass().isInterface();
    }

    // called only by initializing thread; no synchronization required
    void addAlias(final String alias) {
        List<String> aliases = this.aliases;
        if (aliases == null) {
            aliases = this.aliases = new ArrayList<String>(4);
        }
        if ( ! aliases.contains(alias) ) aliases.add(alias);
    }

    @Override
    boolean hasLocalMethod () { return localMethod; }

    void setLocalMethod(boolean flag) { localMethod = flag; }

}
