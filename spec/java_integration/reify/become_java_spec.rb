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

    ReifyInterfacesClass1.to_java(jclass).name.should == "rubyobj.ReifyInterfacesClass1"
    ReifyInterfacesClass1::ReifyInterfacesClass2.to_java(jclass).name.should == "rubyobj.ReifyInterfacesClass1.ReifyInterfacesClass2"

    # checking that the packages are valid for Java's purposes
    lambda do
      ReifyInterfacesClass1.new
      ReifyInterfacesClass1::ReifyInterfacesClass2.new
    end.should_not raise_error
  end
  
  it "creates static methods for Ruby class methods" do
    cls = Class.new do
      class << self
        def blah
        end
      end
    end
    
    java_class = cls.become_java!
    
    method = java_class.declared_methods.select {|m| m.name == "blah"}[0]
    method.name.should == "blah"
    method.return_type.should == org.jruby.runtime.builtin.IRubyObject.java_class
    method.parameter_types.length.should == 0
  end

  it "supports fully reifying a deep class hierarchy" do
    class BottomOfTheStack ; end
    class MiddleOfTheStack < BottomOfTheStack ; end
    class TopOfTheStack < MiddleOfTheStack ; end
 
    java_class = TopOfTheStack.become_java!
  end

  it "supports reification of annotations and signatures on static methods without parameters" do

    cls = Class.new do
      class << self
        def blah_no_args()
        end

        add_method_signature( "blah_no_args", [java.lang.Integer] )
        add_method_annotation( "blah_no_args", java.lang.Deprecated => {} )
      end
    end

    java_class = cls.become_java!()
    method = java_class.declared_methods.select {|m| m.name == "blah_no_args"}[0]
    method.should_not be_nil
    method.return_type.should == java.lang.Integer.java_class

    anno = method.get_annotation( java.lang.Deprecated.java_class )
    anno.should_not be_nil
  end

  it "supports reification of annotations and signatures on static methods with parameters" do

    cls = Class.new do
      class << self
        def blah_with_args(arg_one,arg_two)
        end

        add_method_signature( "blah_with_args", [java.lang.Integer, java.lang.String, java.lang.Float] )
        add_method_annotation( "blah_with_args", java.lang.Deprecated => {} )

      end
    end

    java_class = cls.become_java!()
    method = java_class.declared_methods.select {|m| m.name == "blah_with_args"}[0]
    method.should_not be_nil
    method.return_type.should == java.lang.Integer.java_class

    anno = method.get_annotation( java.lang.Deprecated.java_class )
    anno.should_not be_nil
  end

end
