require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

language_version __FILE__, "define_method"

class DefineMethodSpecClass
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

  ruby_bug "redmine:2117", "1.8.7" do
    it "defines a method on a singleton class" do
      klass = Class.new
      class << klass
        def test_method
          :foo
        end
      end
      child = Class.new(klass)
      sc = class << child; self; end
      sc.send :define_method, :another_test_method, klass.method(:test_method).unbind
      child.another_test_method.should == :foo
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

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if frozen" do
      lambda {
        Class.new { freeze; define_method(:foo) {} }
      }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError if frozen" do
      lambda {
        Class.new { freeze; define_method(:foo) {} }
      }.should raise_error(RuntimeError)
    end
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

    ruby_version_is ""..."1.9" do
      it "returns the value computed by the block when passed one argument" do
        @klass.new.m(1).should == :called
      end

      it "returns the value computed by the block when passed two arguments" do
        @klass.new.m(1, 2).should == :called
      end
    end

    ruby_version_is "1.9" do
      it "raises an ArgumentError when passed one argument" do
        lambda { @klass.new.m 1 }.should raise_error(ArgumentError)
      end

      it "raises an ArgumentError when passed two arguments" do
        lambda { @klass.new.m 1, 2 }.should raise_error(ArgumentError)
      end
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

    ruby_version_is ""..."1.9" do
      it "receives nil as the argument when passed zero arguments" do
        @klass.new.m().should be_nil
      end

      it "receives nil as the argument when passed zero arguments and a block" do
        @klass.new.m() { :computed }.should be_nil
      end

      it "returns the value computed by the block when passed two arguments" do
        @klass.new.m(1, 2).should == [1, 2]
      end
    end

    ruby_version_is "1.9" do
      it "raises an ArgumentError when passed zero arguments" do
        lambda { @klass.new.m }.should raise_error(ArgumentError)
      end

      it "raises an ArgumentError when passed zero arguments and a block" do
        lambda { @klass.new.m { :computed } }.should raise_error(ArgumentError)
      end

      it "raises an ArgumentError when passed two arguments" do
        lambda { @klass.new.m 1, 2 }.should raise_error(ArgumentError)
      end
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
