package org.jruby.javasupport;

import org.jruby.RubyClass;

public interface RubyToJavaClassMap {
    public String getRubyClassNameForJavaClass (Class javaClass);
//    public RubyClass getRubyClassForJavaClass (Class javaClass);

    public Class getJavaClassForRubyClass (RubyClass rubyClass);
//    public Class getJavaClassForRubyClassName (String rubyClassName);
}

