require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#instance_variable_set" do
  it "sets the value of the specified instance variable" do
    class Dog
      def initialize(p1, p2)
        @a, @b = p1, p2
      end
    end
    Dog.new('cat', 99).instance_variable_set(:@a, 'dog').should == "dog"
  end

  it "sets the value of the instance variable when no instance variables exist yet" do
    class NoVariables; end
    NoVariables.new.instance_variable_set(:@a, "new").should == "new"
  end

  it "raises a NameError exception if the argument is not of form '@x'" do
    class NoDog; end
    lambda { NoDog.new.instance_variable_set(:c, "cat") }.should raise_error(NameError)
  end

  it "sets the value of the instance variable if argument is '@'" do
    class DogAt; end
    DogAt.new.instance_variable_set(:'@', "cat").should == "cat"
  end

  ruby_version_is ""..."1.9" do
    it "raises an ArgumentError if the instance variable name is a Fixnum" do
      lambda { "".instance_variable_set(1, 2) }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a TypeError if the instance variable name is a Fixnum" do
      lambda { "".instance_variable_set(1, 2) }.should raise_error(TypeError)
    end
  end

  it "raises a TypeError if the instance variable name is an object that does not respond to to_str" do
    class KernelSpecs::A; end
    lambda { "".instance_variable_set(KernelSpecs::A.new, 3) }.should raise_error(TypeError)
  end

  it "raises a NameError if the passed object, when coerced with to_str, does not start with @" do
    class KernelSpecs::B
      def to_str
        ":c"
      end
    end
    lambda { "".instance_variable_set(KernelSpecs::B.new, 4) }.should raise_error(NameError)
  end

  it "raises a NameError if pass an object that cannot be a symbol" do
    lambda { "".instance_variable_set(:c, 1) }.should raise_error(NameError)
  end

  it "accepts as instance variable name any instance of a class that responds to to_str" do
    class KernelSpecs::C
      def initialize
        @a = 1
      end
      def to_str
        "@a"
      end
    end
    KernelSpecs::C.new.instance_variable_set(KernelSpecs::C.new, 2).should == 2
  end
end
