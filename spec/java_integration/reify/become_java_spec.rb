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
    interfaces = java_class.interfaces
    expect( interfaces ).to include(java.lang.Runnable.java_class)
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

    java_class = TopRightOfTheStack.to_java.getReifiedRubyClass
    expect(java_class).not_to be_nil
  end

  it "supports fully reifying a deep class hierarchy from java parents" do
    class BottomOfTheJStack < java.util.ArrayList ; end
    class MiddleOfTheJStack < BottomOfTheJStack ; end
    class TopLeftOfTheJStack < MiddleOfTheJStack ; end
    class TopRightOfTheJStack < MiddleOfTheJStack ;def size; super + 3; end ; end

    java_class = TopLeftOfTheJStack.become_java!
    expect(java_class).not_to be_nil

    java_class = TopRightOfTheJStack.become_java!
    expect(java_class).not_to be_nil

    java_class = TopRightOfTheJStack.to_java.getReifiedJavaClass
    expect(java_class).not_to be_nil
    
    expect(TopRightOfTheJStack.new).not_to be_nil
    
    expect(TopLeftOfTheJStack.new.size).to eq 0
    expect(TopRightOfTheJStack.new([:a, :b]).size).to eq (2+3)
  end
  
  it "constructs in the right order using the right methods" do
	class BottomOfTheCJStack < java.util.ArrayList
		def initialize(lst)
			lst << :bottom_start
			super()
			lst << :bottom_end
		end
	end
    class MiddleLofTheCJStack < BottomOfTheCJStack 
		def initialize(lst)
			lst << :mid1_start
			super
			lst << :mid1_end
		end
	end
    class MiddleMofTheCJStack < MiddleLofTheCJStack 
		def initialize(lst)
			lst << :mid2_error
			super
			lst << :mid2_end_error
		end
		configure_java_class ctor_name: :reconfigured
		def reconfigured(lst)
			lst << :mid2_good_start
			super
			lst << :mid2_good_end
		end
	end
    class MiddleUofTheCJStack < MiddleMofTheCJStack 
		#default init
	end
	class TopOfTheCJStack < MiddleUofTheCJStack
		def initialize(lst)
		lst << :top_start
		super
		lst << :top_end
		end
	end
	
	trace = []
    expect(TopOfTheCJStack.new(trace)).not_to be_nil
    expect(trace).to eql([:top_start, :mid2_good_start, :mid1_start, :bottom_start, :bottom_end, :mid1_end, :mid2_good_end, :top_end])
  end
  
  it "supports reification of java classes with interfaces" do
	clz = Class.new(java.lang.Exception) do
		include java.util.Iterator
		def initialize(array)
			@array = array
			super(array.inspect)
			@at=0
			@loop = false
		end
		def hasNext
			@at<@array.length
		end
		def next()
			@array[@array.length - 1 - @at].tap{@at+=1}
		end
		
		def remove
			raise java.lang.StackOverflowError.new if @loop
			@loop = true
			begin
				@array<< :fail1
				super
				@array << :fail2
			rescue java.lang.UnsupportedOperationException => uo
				@array = [:success]
			rescue java.lang.StackOverflowError => so
				@array << :failSO
			end
			@loop = false
		end
	end

	gotten = []
	clz.new([:a, :b, :c]).forEachRemaining { |k| gotten << k }
	expect(gotten).to eql([:c,:b, :a])
	expect(clz.new([:a, :b, :c]).message).to eql("[:a, :b, :c]")

	pending "GH#6479 + reification not yet hooked up"
	obj = clz.new(["fail3"])
	obj.remove
	gotten = []
	obj.forEachRemaining { |k| gotten << k }
	expect(gotten).to eql([:success])
	expect(gotten.length).to eql(2)
  end
  
  it "supports reification of ruby classes with interfaces" do
    pending "GH#6479 + reification not yet hooked up"

	clz = Class.new do
		include java.util.Iterator
		def initialize(array)
			@array = array
			@at=0
			@loop = false
		end
		def hasNext
			@at<@array.length
		end
		def next()
			@array[@array.length - 1 - @at].tap{@at+=1}
		end
		
		def remove
			raise java.lang.StackOverflowError.new if @loop
			@loop = true
			begin
				@array<< :fail1
				super
				@array << :fail2
			rescue java.lang.UnsupportedOperationException => uo
				@array = [:success]
			rescue java.lang.StackOverflowError => so
				@array << :failSO
			end
			@loop = false
		end
	end

	obj = clz.new(["fail3"])
	obj.remove
	gotten = []
	obj.forEachRemaining { |k| gotten << k }
	expect(gotten).to eql([:success])
	expect(gotten.length).to eql(2)
  end
  
  
  it "supports reification of concrete classes with non-standard construction" do
  
	clz = Class.new(java.lang.Exception) do
		def jinit(str, seq)
			@seq = seq
			super("foo: #{str}")
			seq << :jinit
		end
		
		def initialize(str, foo)
			@seq << :init
			@seq << str
			@seq << foo
		end
		
		def self.new(seq, str)
			obj = allocate
			seq << :new
			obj.__jallocate!(str, seq)
			seq << :ready
			obj
		end
		
		configure_java_class ctor_name: :jinit
	end
	
	clz.become_java!
	
	lst = []
	obj = clz.new(lst, :bar)
	expect(obj).not_to be_nil
	expect(lst).to eq([:new, :jinit, :ready])
	expect(obj.message).to eq("foo: bar")
	obj.send :initialize, :x, "y"
	expect(lst).to eq([:new, :jinit, :ready, :init, :x, "y"])
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
  
  it "Errors on unimplemented methods in abstract/interfaces" do
    expect { Class.new do; include java.util.Iterator; end.hasNext }.to raise_error(NoMethodError)
    expect { Class.new(java.lang.Exception) do; include java.util.Iterator; end.hasNext }.to raise_error(NoMethodError)
    expect { Class.new(java.util.AbstractList) do; end.get(0) }.to raise_error(NoMethodError)
  end
  
  it "Errors on invalid logic jumps" do
    expect { Class.new(java.util.AbstractList) do; def initialize();while true; super([]); fail_by_not_existing; end; end; end.new }.to raise_error(RuntimeError)
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

  it 'has a similar Java class name' do
    ReifiedSample.become_java!
    klass = ReifiedSample.java_class
    expect( klass.getName ).to eql 'rubyobj.ReifiedSample'
    klass = Class.new(ReifiedSample)
    hexid = klass.inspect.match(/(0x[0-9a-f]+)/)[1]
    klass = klass.become_java!
    expect( klass.getName ).to match /^rubyobj\.Class_#{hexid}/ # rubyobj.Class_0x599f1b7
  end

  it 'works when reflecting annotations' do
    klass = Class.new(ReifiedSample)
    klass.add_class_annotations(Java::java_integration.fixtures.ServiceAnnotation => {
        'service' => Class.new(Java::java_integration.fixtures.Service).become_java!(false)
    })
    klass = klass.become_java!(false)
    annotation = Reflector.getDeclaredAnnotation(klass, Java::java_integration.fixtures.ServiceAnnotation)
    expect( annotation ).not_to be_nil
    expect( annotation.service ).not_to be_nil
  end

  describe "java fields" do
    let(:klass) { Class.new(&fields) }
    let(:fields) { proc { java_field "java.lang.String foo" } }

    subject { klass.become_java! }

    it "adds specified fields to java_class" do
      expect(subject.get_declared_fields.map { |f| f.get_name }).to eq(%w(ruby rubyClass foo))
    end

    it "lets you write to the fields" do
      subject

      expect(klass.new).to respond_to :foo
      expect(klass.new).to respond_to :foo=

      field = subject.get_declared_fields.to_a.detect { |f| f.get_name == "foo" }
      instance = klass.new
      instance.foo = "String Value"
      expect(instance.foo).to eq("String Value")
      expect(field.get(instance)).to eq("String Value")
    end

    context "many fields" do
      let(:fields) { proc {
        java_field "java.lang.String foo"
        java_field "java.lang.String bar"
        java_field "java.lang.String baz"
        java_field "java.lang.StringBuilder zot"
        java_field "int intField"
        java_field "byte[] byteField"
        java_field "short shortField"
        java_field "float floatField"
        java_field "double doubleField"
      }}
      it "keeps the ordering as specified" do
        fields = subject.get_declared_fields.map(&:name)
        expect(fields).to eq(%w(ruby rubyClass foo bar baz zot intField byteField shortField floatField doubleField))
      end
    end
  end

end
