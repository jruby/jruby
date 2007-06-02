require File.dirname(__FILE__) + '/../spec_helper'

# class methods
# method_added, new
 
# ==, ===, =~, __id__, __send__, class, class_def, clone,
# copy_instance_variables_from, dclone, deep_clone, display, dup,
# enum_for, eql?, equal?, expects, extend, freeze, frozen?, hash, id,
# inspect, inspect_for_expectation_not_met_error, instance_eval,
# instance_exec, instance_of?, instance_variable_get,
# instance_variable_set, instance_variables, is_a?, kind_of?,
# meta_def, meta_eval, metaclass, method, methods, mocha_inspect,
# nil?, object_id, parse_tree, private_methods, protected_methods,
# public_methods, remove_instance_variable, respond_to?, returning,
# send, singleton_method_added, singleton_method_removed,
# singleton_method_undefined, singleton_methods, source, stubs,
# taint, tainted?, to_a, to_enum, to_json, to_s, to_yaml,
# to_yaml_properties, to_yaml_style, type, untaint, verify

context "Object class method" do
  specify "new should create a new Object" do
    Object.new.class.should == Object
  end
end

context "Object instance method" do
  specify "send should invoke the named method" do
    class Foo
      def bar
        'done'
      end
    end
    Foo.new.send(:bar).should == 'done'
  end

  specify "send should invoke a class method if called on a class" do
    class Foo
      def self.bar
        'done'
      end
    end
    Foo.send(:bar).should == 'done'
  end

  specify "send should raise NoMethodError if the corresponding method can't be found" do
    class Foo
      def bar
        'done'
      end
    end
    should_raise(NoMethodError) { Foo.new.send(:baz) }
  end

  specify "send should raise NoMethodError if the corresponding singleton method can't be found" do
    class Foo
      def self.bar
        'done'
      end
    end
    should_raise(NoMethodError) { Foo.send(:baz) }
  end
  
  specify "freeze should prevent self from being further modified" do
    module Mod; end
    o = Object.new
    o.freeze
    should_raise(TypeError) { o.extend Mod }
  end
  
  specify "freeze should have no effect on immediate values" do
    a = nil
    b = true
    c = false
    d = 1
    a.freeze
    b.freeze
    c.freeze
    d.freeze
    a.frozen?.should == false
    b.frozen?.should == false
    c.frozen?.should == false
    d.frozen?.should == false
  end
  
  specify "frozen? should return true if self is frozen" do
    o = Object.new
    p = Object.new
    p.freeze
    o.frozen?.should == false
    p.frozen?.should == true
  end
  
  specify "taint should set self to be tainted" do
    Object.new.taint.tainted?.should == true
  end
  
  specify "taint should have no effect on immediate values" do
    a = nil
    b = true
    c = false
    d = 1
    a.tainted?.should == false
    b.tainted?.should == false
    c.tainted?.should == false
    d.tainted?.should == false
  end
  
  specify "tainted? should return true if Object is tainted" do
    o = Object.new
    p = Object.new
    p.taint
    o.tainted?.should == false
    p.tainted?.should == true
  end
  
  specify "instance_eval with no arguments should expect a block" do
    should_raise(ArgumentError) { "hola".instance_eval }
  end
  
  specify "instance_eval with a block should take no arguments" do
    should_raise(ArgumentError) { "hola".instance_eval(4, 5) { |a,b| a + b } }
  end
  
  specify "instance_eval with a block should pass the object to the block" do
    "hola".instance_eval { |o| o.size }.should == 4
  end
  
  specify "instance_eval with a block should bind self to the receiver" do
    s = "hola"
    (s == s.instance_eval { self }).should == true
  end
  
  specify "instance_eval with a block should execute in the context of the receiver" do
    "Ruby-fu".instance_eval { size }.should == 7
  end

  specify "instance_eval with a block should have access to receiver's instance variables" do
    class Klass
      def initialize
        @secret = 99
      end
    end
    Klass.new.instance_eval { @secret }.should == 99
  end
  
  specify "instance_eval with string argument should execute on the receiver context" do
    "hola".instance_eval("size").should == 4
  end

  specify "instance_eval with string argument should bind self to the receiver" do
    o = Object.new
    (o == o.instance_eval("self")).should == true
  end

  specify "instance_eval with string argument should have access to receiver's instance variables" do
    class Klass
      def initialize
        @secret = 99
      end
    end
    Klass.new.instance_eval("@secret").should == 99
  end
  
  specify "instance_variable_get should return the value of the instance variable" do
    class Fred 
      def initialize(p1, p2) 
        @a, @b = p1, p2 
      end 
    end 
    fred = Fred.new('cat', 99) 
    fred.instance_variable_get(:@a).should == "cat"
    fred.instance_variable_get("@b").should == 99
  end
  
  specify "instance_variable_get should raise NameError exception if the argument is not of form '@x'" do
    class NoFred; end
    should_raise(NameError) { NoFred.new.instance_variable_get(:c) }
  end

  specify "instance_variable_set should set the value of the specified instance variable" do
    class Dog
      def initialize(p1, p2) 
        @a, @b = p1, p2 
      end 
    end 
    Dog.new('cat', 99).instance_variable_set(:@a, 'dog').should == "dog"
  end
  
  specify "instance_variable_set should set the value of the instance variable when no instance variables exist yet" do
    class NoVariables; end
    NoVariables.new.instance_variable_set(:@a, "new").should == "new"
  end
  
  specify "instance_variable_set should raise NameError exception if the argument is not of form '@x'" do
    class NoDog; end
    should_raise(NameError) { NoDog.new.instance_variable_set(:c, "cat") }
  end

  specify "metaclass should return the object's metaclass" do
    foo = "foo"
    foo.instance_eval "class << self; def meta_test_method; 5; end; end"
    foo.respond_to?(:meta_test_method).should == true
    should_raise(NameError) { "hello".metaclass.method(:meta_test_method) }
  end

  specify "method should return a method object for a valid method" do
    class Foo; def bar; 'done'; end; end
    Foo.new.method(:bar).class.should == Method
  end

  specify "method should return a method object for a valid singleton method" do
    class Foo; def self.bar; 'done'; end; end
    Foo.method(:bar).class.should == Method
  end
 
  specify "method should raise a NameError for an invalid method name" do
    class Foo; def bar; 'done'; end; end
    should_raise(NameError) { Foo.new.method(:baz) }
  end

  specify "method should raise a NameError for an invalid singleton method name" do
    class Foo; def self.bar; 'done'; end; end
    should_raise(NameError) { Foo.method(:baz) }
  end

  specify "respond_to? should indicate if an object responds to a particular message" do
    class Foo; def bar; 'done'; end; end
    Foo.new.respond_to?(:bar).should == true
    Foo.new.respond_to?(:baz).should == false
  end

  specify "respond_to? should indicate if a singleton object responds to a particular message" do
    class Foo; def self.bar; 'done'; end; end
    Foo.respond_to?(:bar).should == true
    Foo.respond_to?(:baz).should == false
  end
end

describe "Object#instance_variable_get" do
  it "should raise ArgumentError if the instance variable name is a Fixnum" do
    should_raise(ArgumentError) { "".instance_variable_get(1) }
  end
  
  it "should raise TypeError if the instance variable name is an object that does not respond to to_str" do
    class A; end
    should_raise(TypeError) { "".instance_variable_get(A.new) }
  end
  
  it "should raise NameError if the passed object, when coerced with to_str, does not start with @" do
    class B
      def to_str
        ":c"
      end
    end
    should_raise(NameError) { "".instance_variable_get(B.new) }
  end
  
  it "should raise NameError if pass an object that cannot be a symbol" do
    should_raise(NameError) { "".instance_variable_get(:c) }
  end
  
  it "should accept as instance variable name any instance of a class that responds to to_str" do
    class C
      def initialize
        @a = 1
      end
      def to_str
        "@a"
      end
    end
    C.new.instance_variable_get(C.new).should == 1
  end
end

describe "Object#instance_variable_set" do
  it "should raise ArgumentError if the instance variable name is a Fixnum" do
    should_raise(ArgumentError) { "".instance_variable_set(1, 2) }
  end
  
  it "should raise TypeError if the instance variable name is an object that does not respond to to_str" do
    class A; end
    should_raise(TypeError) { "".instance_variable_set(A.new, 3) }
  end
  
  it "should raise NameError if the passed object, when coerced with to_str, does not start with @" do
    class B
      def to_str
        ":c"
      end
    end
    should_raise(NameError) { "".instance_variable_set(B.new, 4) }
  end
  
  it "should raise NameError if pass an object that cannot be a symbol" do
    should_raise(NameError) { "".instance_variable_set(:c, 1) }
  end
  
  it "should accept as instance variable name any instance of a class that responds to to_str" do
    class C
      def initialize
        @a = 1
      end
      def to_str
        "@a"
      end
    end
    C.new.instance_variable_set(C.new, 2).should == 2
  end
end
