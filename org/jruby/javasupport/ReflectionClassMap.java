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

    public Class getJavaClassForRubyClass (RubyClass rubyClass)
    {
        while (rubyClass != null) {
            try {
                String rubyClassName = rubyClass.getClassname();
                String javaClassName = javaPackage + "." + rubyClassName;
                return Class.forName(javaClassName);
            } catch (ClassNotFoundException ex) { }

            try {
                rubyClass = rubyClass.superclass();
            } catch (NullPointerException ex) {
                rubyClass = null;
            }

            if (rubyClass != null && rubyClass.getClassname() == null)
              rubyClass = null;
        }
        return null;
    }
}

