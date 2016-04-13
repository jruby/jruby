require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

java_import 'java_integration.fixtures.Reflector'

describe "JRuby class reification" do

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
    expect(java_class.interfaces).to include(java.lang.Runnable.java_class)
  end

  it "uses the nesting of the class for its package name" do
    class ReifyInterfacesClass1
      class ReifyInterfacesClass2
      end
    end
    ReifyInterfacesClass1.become_java!
    ReifyInterfacesClass1::ReifyInterfacesClass2.become_java!

    jclass = java.lang.Class

    expect(ReifyInterfacesClass1.to_java(jclass).name).to eq("rubyobj.ReifyInterfacesClass1")
    expect(ReifyInterfacesClass1::ReifyInterfacesClass2.to_java(jclass).name).to eq("rubyobj.ReifyInterfacesClass1.ReifyInterfacesClass2")

    # checking that the packages are valid for Java's purposes
    expect do
      ReifyInterfacesClass1.new
      ReifyInterfacesClass1::ReifyInterfacesClass2.new
    end.not_to raise_error
  end

  it "creates static methods for reified Ruby class methods" do
    klass = Class.new do
      class << self
        def foo; 'FOO' end
      end
      def self.bar(arg = 'baz'); "BAR-#{arg.upcase rescue arg.inspect}" end
    end

    java_class = klass.become_java!

    method = java_class.declared_methods.find { |m| m.name == 'foo' }
    expect(method.return_type).to eql org.jruby.runtime.builtin.IRubyObject.java_class
    expect(method.parameter_types.length).to eql 0 # foo()

    expect( Reflector.invoke(nil, method) ).to eql 'FOO'

    method = java_class.declared_methods.find { |m| m.name == 'bar' }

    expect( method.return_type ).to eql org.jruby.runtime.builtin.IRubyObject.java_class
    expect( method.parameter_types.length ).to eql 1 # bar(org.jruby.runtime.builtin.IRubyObject[])
    expect( method.parameter_types[0] ).to eql org.jruby.runtime.builtin.IRubyObject[].java_class
    expect( method.isVarArgs ).to be true

    expect( Reflector.invoke(nil, method) ).to eql 'BAR-BAZ'
    args = org.jruby.runtime.builtin.IRubyObject[1].new
    expect( Reflector.invoke(nil, method, args) ).to eql 'BAR-nil'
    args = org.jruby.runtime.builtin.IRubyObject[1].new; args[0] = 'zZz'.to_java(org.jruby.runtime.builtin.IRubyObject)
    expect( Reflector.invoke(nil, method, args) ).to eql 'BAR-ZZZ'
  end

  it "supports fully reifying a deep class hierarchy" do
    class BottomOfTheStack ; end
    class MiddleOfTheStack < BottomOfTheStack ; end
    class TopLeftOfTheStack < MiddleOfTheStack ; end
    class TopRightOfTheStack < MiddleOfTheStack ; end

    java_class = TopLeftOfTheStack.become_java!
    expect(java_class).not_to be_nil

    java_class = TopRightOfTheStack.become_java!
    expect(java_class).not_to be_nil
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
    expect(method).not_to be_nil
    expect(method.return_type).to eq(java.lang.Integer.java_class)

    anno = method.get_annotation( java.lang.Deprecated.java_class )
    expect(anno).not_to be_nil
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
    expect(method).not_to be_nil
    expect(method.return_type).to eq(java.lang.Integer.java_class)

    anno = method.get_annotation( java.lang.Deprecated.java_class )
    expect(anno).not_to be_nil
  end

  it "allows loading reified classes into the main JRubyClassLoader" do
    class JRUBY5564; end
    a_class = JRUBY5564.become_java!(false)

    # load the java class from the classloader
    klass = java.lang.Thread.current_thread.getContextClassLoader
    expect(klass.load_class(a_class.get_name)).to eq(a_class)
  end

  class ReifiedSample
    def hello; 'Sayonara from Ruby' end
    java_signature "java.lang.String ahoy()"
    def ahoy; 'Ahoy There!' end

    def hoja(arg); 'Hoja ' + arg.to_s end
    def szia(arg1, arg2, arg3); return "Szia #{arg1} #{arg2} #{arg3}" end

    def greet(*args); "Greetings #{args.inspect}!" end

    java_signature "java.lang.String ola(java.lang.String[] args)"
    def ola(*args); "OLA #{args.join(' ')}" end
  end

  it "handles argument count for reified class methods" do
    j_class = ReifiedSample.become_java!; sample = ReifiedSample.new

    methods = Reflector.findMethods(sample, 'hello')
    expect( methods.size ).to eql 1; method = methods[0]
    expect( method.isVarArgs ).to be false
    expect( Reflector.invoke(sample, 'hello') ).to eql 'Sayonara from Ruby'
    expect( Reflector.invoke(sample, j_class, 'hello') ).to eql 'Sayonara from Ruby'

    methods = Reflector.findMethods(sample, 'ahoy')
    expect( methods.size ).to eql 1; method = methods[0]
    expect( method.isVarArgs ).to be false
    expect( j_class.newInstance.ahoy ).to eql 'Ahoy There!'

    methods = Reflector.findMethods(sample, 'hoja')
    expect( methods.size ).to eql 1; method = methods[0]
    expect( method.isVarArgs ).to be false
    expect( j_class.newInstance.hoja('Ferko') ).to eql 'Hoja Ferko'
    expect { j_class.newInstance.szia('Ferko', 'Janko') }.to raise_error(ArgumentError)

    methods = Reflector.findMethods(sample, 'szia')
    expect( methods.size ).to eql 1; method = methods[0]
    expect( method.isVarArgs ).to be false
    expect( j_class.newInstance.szia('Jozko', 'Janko', 'Ferko') ).to eql 'Szia Jozko Janko Ferko'
    expect { j_class.newInstance.szia('Jozko', 'Janko') }.to raise_error(ArgumentError)

    method = Reflector.resolveMethod(sample, 'greet', org.jruby.runtime.builtin.IRubyObject[])
    expect( method.isVarArgs ).to be true
    expect( Reflector.invoke(sample, method) ).to eql 'Greetings []!'
    expect( j_class.newInstance.greet ).to eql 'Greetings []!'
    expect( j_class.newInstance.greet('Janko') ).to eql 'Greetings ["Janko"]!'

    method = Reflector.resolveMethod(sample, 'ola', java.lang.String[])
    expect( method.isVarArgs ).to be true
    expect( j_class.newInstance.ola('Jozko') ).to eql 'OLA Jozko'
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
      expect(subject.get_declared_fields.map { |f| f.get_name }).to eq(%w(ruby rubyClass foo))
    end

    it "lets you write to the fields" do
      subject

      expect(cls.new).to respond_to :foo
      expect(cls.new).to respond_to :foo=

      field = subject.get_declared_fields.to_a.detect { |f| f.get_name == "foo" }
      instance = cls.new
      instance.foo = "String Value"
      expect(instance.foo).to eq("String Value")
      expect(field.get(instance)).to eq("String Value")
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
        expect(subject.get_declared_fields.map { |f| f.get_name }).to eq(%w(ruby rubyClass foo bar baz zot allegro))
      end
    end
  end
end
