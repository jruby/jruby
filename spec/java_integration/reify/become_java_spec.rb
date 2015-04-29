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
    class TopLeftOfTheStack < MiddleOfTheStack ; end
    class TopRightOfTheStack < MiddleOfTheStack ; end
 
    java_class = TopLeftOfTheStack.become_java!
    java_class.should_not be_nil
    
    java_class = TopRightOfTheStack.become_java!
    java_class.should_not be_nil
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

  it "allows loading reified classes into the main JRubyClassLoader" do
    class JRUBY5564; end
    a_class = JRUBY5564.become_java!(false)

    # load the java class from the jruby-classloader
    # this diverts from the original issue 5564 since
    # JRuby can run without any context_class_loader involved
    cl = JRuby.runtime.jruby_class_loader
    cl.load_class(a_class.get_name).should == a_class
  end

  describe "java fields" do
    let(:cls) {
      Class.new(&fields)
    }
    let(:fields) { proc {
      java_field "java.lang.String foo"
    } }

    subject { cls.become_java! }

    it "adds specified fields to java_class" do
      subject.get_declared_fields.map { |f| f.get_name }.should == %w(ruby rubyClass foo)
    end

    it "lets you write to the fields" do
      subject

      cls.new.should respond_to :foo
      cls.new.should respond_to :foo=

      field = subject.get_declared_fields.to_a.detect { |f| f.get_name == "foo" }
      instance = cls.new
      instance.foo = "String Value"
      instance.foo.should == "String Value"
      field.get(instance).should == "String Value"
    end

    context "many fields" do
      let(:fields) { proc {
        java_field "java.lang.String foo"
        java_field "java.lang.String bar"
        java_field "java.lang.String baz"
        java_field "java.lang.String zot"
        java_field "java.lang.String allegro"
      }}
      it "keeps the ordering as specified" do
        subject.get_declared_fields.map { |f| f.get_name }.should == %w(ruby rubyClass foo bar baz zot allegro)
      end
    end
  end
end
