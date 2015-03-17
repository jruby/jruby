require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

class DefineMethodSpecClass
end

describe "passed { |a, b = 1|  } creates a method that" do
  before :each do
    @klass = Class.new do
      define_method(:m) { |a, b = 1| return a, b }
    end
  end

  it "raises an ArgumentError when passed zero arguments" do
    lambda { @klass.new.m }.should raise_error(ArgumentError)
  end

  it "has a default value for b when passed one argument" do
    @klass.new.m(1).should == [1, 1]
  end

  it "overrides the default argument when passed two arguments" do
    @klass.new.m(1, 2).should == [1, 2]
  end

  it "raises an ArgumentError when passed three arguments" do
    lambda { @klass.new.m(1, 2, 3) }.should raise_error(ArgumentError)
  end
end

describe "Module#define_method when given an UnboundMethod" do
  it "passes the given arguments to the new method" do
    klass = Class.new do
      def test_method(arg1, arg2)
        [arg1, arg2]
      end
      define_method(:another_test_method, instance_method(:test_method))
    end

    klass.new.another_test_method(1, 2).should == [1, 2]
  end

  it "adds the new method to the methods list" do
    klass = Class.new do
      def test_method(arg1, arg2)
        [arg1, arg2]
      end
      define_method(:another_test_method, instance_method(:test_method))
    end
    klass.new.should have_method(:another_test_method)
  end

  describe "defining a method on a singleton class" do
    before do
      klass = Class.new
      class << klass
        def test_method
          :foo
        end
      end
      child = Class.new(klass)
      sc = class << child; self; end
      sc.send :define_method, :another_test_method, klass.method(:test_method).unbind

      @class = child
    end

    it "doesn't raise TypeError when calling the method" do
      @class.another_test_method.should == :foo
    end
  end
end

describe "Module#define_method when name is :initialize" do
  describe "passed a block" do
    it "sets visibility to private when method name is :initialize" do
      klass = Class.new do
        define_method(:initialize) { }
      end
      klass.should have_private_instance_method(:initialize)
    end
  end

  describe "given an UnboundMethod" do
    it "sets the visibility to private when method is named :initialize" do
      klass = Class.new do
        def test_method
        end
        define_method(:initialize, instance_method(:test_method))
      end
      klass.should have_private_instance_method(:initialize)
    end
  end
end

describe "Module#define_method" do
  it "defines the given method as an instance method with the given name in self" do
    class DefineMethodSpecClass
      def test1
        "test"
      end
      define_method(:another_test, instance_method(:test1))
    end

    o = DefineMethodSpecClass.new
    o.test1.should == o.another_test
  end

  it "calls #method_added after the method is added to the Module" do
    DefineMethodSpecClass.should_receive(:method_added).with(:test_ma)

    class DefineMethodSpecClass
      define_method(:test_ma) { true }
    end
  end

  it "defines a new method with the given name and the given block as body in self" do
    class DefineMethodSpecClass
      define_method(:block_test1) { self }
      define_method(:block_test2, &lambda { self })
    end

    o = DefineMethodSpecClass.new
    o.block_test1.should == o
    o.block_test2.should == o
  end

  it "raises a TypeError when the given method is no Method/Proc" do
    lambda {
      Class.new { define_method(:test, "self") }
    }.should raise_error(TypeError)

    lambda {
      Class.new { define_method(:test, 1234) }
    }.should raise_error(TypeError)

    lambda {
      Class.new { define_method(:test, nil) }
    }.should raise_error(TypeError)
  end

  it "raises an ArgumentError when no block is given" do
    lambda {
      Class.new { define_method(:test) }
    }.should raise_error(ArgumentError)
  end

  it "does not change the arity check style of the original proc" do
    class DefineMethodSpecClass
      prc = Proc.new { || true }
      method = define_method("proc_style_test", &prc)
    end

    obj = DefineMethodSpecClass.new
    lambda { obj.proc_style_test :arg }.should raise_error(ArgumentError)
  end

  it "raises a RuntimeError if frozen" do
    lambda {
      Class.new { freeze; define_method(:foo) {} }
    }.should raise_error(RuntimeError)
  end

  it "accepts a Method (still bound)" do
    class DefineMethodSpecClass
      attr_accessor :data
      def inspect_data
        "data is #{@data}"
      end
    end
    o = DefineMethodSpecClass.new
    o.data = :foo
    m = o.method(:inspect_data)
    m.should be_an_instance_of(Method)
    klass = Class.new(DefineMethodSpecClass)
    klass.send(:define_method,:other_inspect, m)
    c = klass.new
    c.data = :bar
    c.other_inspect.should == "data is bar"
    lambda{o.other_inspect}.should raise_error(NoMethodError)
  end

  it "maintains the Proc's scope" do
    class DefineMethodByProcClass
      in_scope = true
      method_proc = proc { in_scope }

      define_method(:proc_test, &method_proc)
    end

    o = DefineMethodByProcClass.new
    o.proc_test.should be_true
  end

  it "accepts a String method name" do
    klass = Class.new do
      define_method("string_test") do
        "string_test result"
      end
    end

    klass.new.string_test.should == "string_test result"
  end

  it "is private" do
    Module.should have_private_instance_method(:define_method)
  end

  ruby_version_is ""..."2.1" do
    it "returns a Proc" do
      class DefineMethodSpecClass
        method = define_method("return_test") { || true }
        method.is_a?(Proc).should be_true
        # check if it is a lambda:
        lambda {
          method.call :too_many_arguments
        }.should raise_error(ArgumentError)
      end
    end
  end

  ruby_version_is "2.1" do
    it "returns its symbol" do
      class DefineMethodSpecClass
        method = define_method("return_test") { || true }
        method.should == :return_test
      end
    end
  end

  it "allows an UnboundMethod from a module to be defined on a class" do
    DestinationClass = Class.new {
      define_method :bar, ModuleSpecs::UnboundMethodTest.instance_method(:foo)
    }
    DestinationClass.new.should respond_to(:bar)
  end

  it "allows an UnboundMethod from a parent class to be defined on a child class" do
    Parent = Class.new { define_method(:foo) { :bar } }
    ChildClass = Class.new(Parent) {
      define_method :baz, Parent.instance_method(:foo)
    }
    ChildClass.new.should respond_to(:baz)
  end

  it "allows an UnboundMethod from a module to be defined on another unrelated module" do
    DestinationModule = Module.new {
      define_method :bar, ModuleSpecs::UnboundMethodTest.instance_method(:foo)
    }
    DestinationClass = Class.new { include DestinationModule }

    DestinationClass.new.should respond_to(:bar)
  end

  it "raises a TypeError when an UnboundMethod from a child class is defined on a parent class" do
    lambda {
      ParentClass = Class.new { define_method(:foo) { :bar } }
      ChildClass = Class.new(ParentClass) { define_method(:foo) { :baz } }
      ParentClass.send :define_method, :foo, ChildClass.instance_method(:foo)
    }.should raise_error(TypeError)
  end

  it "raises a TypeError when an UnboundMethod from one class is defined on an unrelated class" do
    lambda {
      DestinationClass = Class.new {
        define_method :bar, ModuleSpecs::InstanceMeth.instance_method(:foo)
      }
    }.should raise_error(TypeError)
  end
end

describe "Module#define_method" do
  describe "passed {  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { :called }
      end
    end

    it "returns the value computed by the block when passed zero arguments" do
      @klass.new.m().should == :called
    end

    it "raises an ArgumentError when passed one argument" do
      lambda { @klass.new.m 1 }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed two arguments" do
      lambda { @klass.new.m 1, 2 }.should raise_error(ArgumentError)
    end
  end

  describe "passed { ||  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { || :called }
      end
    end

    it "returns the value computed by the block when passed zero arguments" do
      @klass.new.m().should == :called
    end

    it "raises an ArgumentError when passed one argument" do
      lambda { @klass.new.m 1 }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed two arguments" do
      lambda { @klass.new.m 1, 2 }.should raise_error(ArgumentError)
    end
  end

  describe "passed { |a|  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { |a| a }
      end
    end

    it "raises an ArgumentError when passed zero arguments" do
      lambda { @klass.new.m }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed zero arguments and a block" do
      lambda { @klass.new.m { :computed } }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed two arguments" do
      lambda { @klass.new.m 1, 2 }.should raise_error(ArgumentError)
    end

    it "receives the value passed as the argument when passed one argument" do
      @klass.new.m(1).should == 1
    end

  end

  describe "passed { |*a|  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { |*a| a }
      end
    end

    it "receives an empty array as the argument when passed zero arguments" do
      @klass.new.m().should == []
    end

    it "receives the value in an array when passed one argument" do
      @klass.new.m(1).should == [1]
    end

    it "receives the values in an array when passed two arguments" do
      @klass.new.m(1, 2).should == [1, 2]
    end
  end

  describe "passed { |a, *b|  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { |a, *b| return a, b }
      end
    end

    it "raises an ArgumentError when passed zero arguments" do
      lambda { @klass.new.m }.should raise_error(ArgumentError)
    end

    it "returns the value computed by the block when passed one argument" do
      @klass.new.m(1).should == [1, []]
    end

    it "returns the value computed by the block when passed two arguments" do
      @klass.new.m(1, 2).should == [1, [2]]
    end

    it "returns the value computed by the block when passed three arguments" do
      @klass.new.m(1, 2, 3).should == [1, [2, 3]]
    end
  end

  describe "passed { |a, b|  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { |a, b| return a, b }
      end
    end

    it "returns the value computed by the block when passed two arguments" do
      @klass.new.m(1, 2).should == [1, 2]
    end

    it "raises an ArgumentError when passed zero arguments" do
      lambda { @klass.new.m }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed one argument" do
      lambda { @klass.new.m 1 }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed one argument and a block" do
      lambda { @klass.new.m(1) { } }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed three arguments" do
      lambda { @klass.new.m 1, 2, 3 }.should raise_error(ArgumentError)
    end
  end

  describe "passed { |a, b, *c|  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { |a, b, *c| return a, b, c }
      end
    end

    it "raises an ArgumentError when passed zero arguments" do
      lambda { @klass.new.m }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed one argument" do
      lambda { @klass.new.m 1 }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError when passed one argument and a block" do
      lambda { @klass.new.m(1) { } }.should raise_error(ArgumentError)
    end

    it "receives an empty array as the third argument when passed two arguments" do
      @klass.new.m(1, 2).should == [1, 2, []]
    end

    it "receives the third argument in an array when passed three arguments" do
      @klass.new.m(1, 2, 3).should == [1, 2, [3]]
    end
  end
end

describe "Method#define_method when passed a Method object" do
  before :each do
    @klass = Class.new do
      def m(a, b, *c)
        :m
      end
    end

    @obj = @klass.new
    m = @obj.method :m

    @klass.class_exec do
      define_method :n, m
    end
  end

  it "defines a method with the same #arity as the original" do
    @obj.method(:n).arity.should == @obj.method(:m).arity
  end

  it "defines a method with the same #parameters as the original" do
    @obj.method(:n).parameters.should == @obj.method(:m).parameters
  end
end

describe "Method#define_method when passed an UnboundMethod object" do
  before :each do
    @klass = Class.new do
      def m(a, b, *c)
        :m
      end
    end

    @obj = @klass.new
    m = @klass.instance_method :m

    @klass.class_exec do
      define_method :n, m
    end
  end

  it "defines a method with the same #arity as the original" do
    @obj.method(:n).arity.should == @obj.method(:m).arity
  end

  it "defines a method with the same #parameters as the original" do
    @obj.method(:n).parameters.should == @obj.method(:m).parameters
  end
end
