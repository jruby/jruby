package org.jruby.javasupport;

import java.util.Map;
import java.util.HashMap;
import org.jruby.RubyClass;

public class StaticClassMap implements RubyToJavaClassMap {
    private Map rubyToJavaClassMap = new HashMap();
    private Map javaToRubyClassMap = new HashMap();

    public void registerRubyClass (String rubyClassName, Class javaClass)
    {
        rubyToJavaClassMap.put(rubyClassName, javaClass);
        javaToRubyClassMap.put(javaClass, rubyClassName);
    }

    public String getRubyClassNameForJavaClass (Class javaClass)
    {
        return (String)javaToRubyClassMap.get(javaClass);
    }

    public Class getJavaClassForRubyClass (RubyClass rubyClass)
    {
        String className = rubyClass.getClassname();
        return getJavaClassForRubyClassName(className);
    }

    public Class getJavaClassForRubyClassName (String rubyClassName)
    {
        return (Class)rubyToJavaClassMap.get(rubyClassName);
    }
}

