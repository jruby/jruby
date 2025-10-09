package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jruby.util.StringSupport.startsWith;

/**
* Created by headius on 2/26/15.
*/
public abstract class MethodInstaller extends NamedInstaller {

    final ArrayList<Method> methods = new ArrayList<>(4);
    private List<String> aliases;
    private boolean localMethod;

    public MethodInstaller(String name, int type) { super(name, type); }

    // called only by initializing thread; no synchronization required
    final void addMethod(final Method method, final Class<?> clazz) {
        this.methods.add(method);
        Class<?> declaringClass = method.getDeclaringClass();

        // Only bind this method if it is local to the target class or if it is
        // declared in an interface or non-public superclass.
        localMethod |=
            clazz == declaringClass ||
                    !Modifier.isPublic(declaringClass.getModifiers()) ||
                    declaringClass.isInterface();
    }

    // called only by initializing thread; no synchronization required
    final void addAlias(final String alias) {
        Collection<String> aliases = this.aliases;
        if (aliases == null) {
            aliases = this.aliases = new ArrayList<>(4);
        }
        if ( ! aliases.contains(alias) ) aliases.add(alias);
    }

    void assignAliases(final Map<String, AssignedName> assignedNames) {
        final String name = this.name;
        String rubyCasedName = JavaUtil.getRubyCasedName(name);

        addUnassignedAlias(rubyCasedName, assignedNames, Priority.ALIAS);

        String javaPropertyName = JavaUtil.getJavaPropertyName(name);

        final List<Method> methods = this.methods;

        for ( int i = 0; i < methods.size(); i++ ) {
            final Method method = methods.get(i);
            Class<?>[] argTypes = method.getParameterTypes();
            Class<?> resultType = method.getReturnType();
            int argCount = argTypes.length;

            // Add scala aliases for apply/update to roughly equivalent Ruby names
            if (name.equals("apply")) {
                addUnassignedAlias("[]", assignedNames, Priority.ALIAS);
            } else if (argCount == 2 && name.equals("update")) {
                addUnassignedAlias("[]=", assignedNames, Priority.ALIAS);
            } else if (startsWith(name, '$')) { // Scala aliases for $ method names
                addUnassignedAlias(MethodGatherer.fixScalaNames(name), assignedNames, Priority.ALIAS);
            }

            String rubyPropertyName = null;

            // Add property name aliases
            if (javaPropertyName != null) {
                if (rubyCasedName.startsWith("get_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 0) {  // getFoo      => foo
                        addUnassignedAlias(javaPropertyName, assignedNames, Priority.GET_ALIAS);
                        addUnassignedAlias(rubyPropertyName, assignedNames, Priority.GET_ALIAS);
                    }
                } else if (rubyCasedName.startsWith("set_")) {
                    rubyPropertyName = rubyCasedName.substring(4); // TODO do not add foo? for setFoo (returning boolean)
                    if (argCount == 1) {  // setFoo(Foo) => foo=(Foo)
                        addUnassignedAlias(javaPropertyName + '=', assignedNames, Priority.ALIAS);
                        addUnassignedAlias(rubyPropertyName + '=', assignedNames, Priority.ALIAS);
                    }
                } else if (rubyCasedName.startsWith("is_")) {
                    rubyPropertyName = rubyCasedName.substring(3);
                    // TODO (9.2) should be another check here to make sure these are only for getters
                    // ... e.g. isFoo() and not arbitrary isFoo(param) see GH-4432
                    if (resultType == boolean.class) {  // isFoo() => foo, isFoo(*) => foo(*)
                        addUnassignedAlias(javaPropertyName, assignedNames, Priority.IS_ALIAS);
                        addUnassignedAlias(rubyPropertyName, assignedNames, Priority.IS_ALIAS);
                        // foo? is added bellow
                    }
                }
            }

            // Additionally add ?-postfixed aliases to any boolean methods and properties.
            if (resultType == boolean.class) {
                // isFoo -> isFoo?, contains -> contains?
                addUnassignedAlias(rubyCasedName + '?', assignedNames, Priority.ALIAS);
                if (rubyPropertyName != null) { // isFoo -> foo?
                    addUnassignedAlias(rubyPropertyName + '?', assignedNames, Priority.ALIAS);
                }
            }
        }
    }

    boolean addUnassignedAlias(final String name,
                                       final Map<String, AssignedName> assignedNames,
                                       final Priority aliasType) {

        AssignedName assignedName = assignedNames.get(name);

        if (aliasType.moreImportantThan(assignedName)) {
            addAlias(name);
            assignedNames.put(name, new AssignedName(name, aliasType));
            return true;
        }
        if (aliasType.asImportantAs(assignedName)) {
            addAlias(name);
            return true;
        }

        // TODO: missing additional logic for dealing with conflicting protected fields.

        return false;
    }

    final void removeAlias(final String alias) {
        Collection<String> aliases = this.aliases;
        if (aliases == null) return;
        aliases.remove(alias);
    }

    @Deprecated(since = "10.0.0.0")
    final void defineMethods(RubyModule target, DynamicMethod invoker) {
        defineMethods(target.getCurrentContext(), target, invoker, true);
    }

    @Deprecated(since = "10.0.0.0")
    protected final void defineMethods(RubyModule target, DynamicMethod invoker, boolean checkDups) {
        defineMethods(target.getCurrentContext(), target, invoker, checkDups);
    }

    protected final void defineMethods(ThreadContext context, RubyModule target, DynamicMethod invoker, boolean checkDups) {
        String oldName = this.name;
        target.addMethod(context, oldName, invoker);

        List<String> aliases = this.aliases;
        if ( aliases != null && isPublic() ) {
            for (int i = 0; i < aliases.size(); i++) {
                String name = aliases.get(i);
                if (checkDups && oldName.equals(name)) continue;
                target.addMethod(context, name, invoker);
            }
        }
    }

    @Override
    boolean hasLocalMethod () { return localMethod; }

    void setLocalMethod(boolean flag) { localMethod = flag; }

}
