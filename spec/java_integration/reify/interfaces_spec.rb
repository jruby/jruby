require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

describe "JRuby class reification" do
  jclass = java.lang.Class

  class RubyRunnable
    include java.lang.Runnable
    def run
    end
  end

  it "should add the included Java interfaces to the reified class" do
    RubyRunnable.module_eval do
      add_method_signature("run", [java.lang.Void::TYPE])
    end
    java_class = RubyRunnable.become_java!
    java_class.interfaces.should include(java.lang.Runnable.java_class)
  end

  it "uses the nesting of the class for its package name" do
    class ReifyInterfacesClass1
      class ReifyInterfacesClass2
      end
    end
    ReifyInterfacesClass1.become_java!
    ReifyInterfacesClass1::ReifyInterfacesClass2.become_java!

    ReifyInterfacesClass1.to_java(jclass).name.should == "ruby.ReifyInterfacesClass1"
    ReifyInterfacesClass1::ReifyInterfacesClass2.to_java(jclass).name.should == "ruby.ReifyInterfacesClass1.ReifyInterfacesClass2"

    # checking that the packages are valid for Java's purposes
    lambda do
      ReifyInterfacesClass1.new
      ReifyInterfacesClass1::ReifyInterfacesClass2.new
    end.should_not raise_error
  end
end
