package org.jruby.javasupport;

import org.jruby.RubyClass;

public class ReflectionClassMap implements RubyToJavaClassMap {
    private String javaPackage = null;

    public ReflectionClassMap (String pkgName)
    {
        this.javaPackage = pkgName;
    }

    public ReflectionClassMap (Package pkg)
    {
        this.javaPackage = pkg.getName();
    }

    public String getRubyClassNameForJavaClass (Class javaClass)
    {
        String name = javaClass.getName();

        if (name.lastIndexOf(".") >= 0)
            name = name.substring(name.lastIndexOf(".") + 1);

        return name;
    }

    public Class getJavaClassForRubyClass(RubyClass rubyClass) {
        while (rubyClass != null) {
            try {
                // TODO: This should get real class name of the class implementing
                // the name.
                String rubyClassName = rubyClass.getName();
                String javaClassName = javaPackage + "." + rubyClassName;
                return Class.forName(javaClassName);
            } catch (ClassNotFoundException ex) {
            }

            if (rubyClass.superclass().isNil()) {
                break;
            }
            rubyClass = (RubyClass) rubyClass.superclass();

            if (rubyClass != null && rubyClass.getName() == null) {
                break;
            }
        }
        return null;
    }
}

