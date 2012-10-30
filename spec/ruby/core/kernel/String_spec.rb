require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe :kernel_String, :shared => true do
  it "converts nil to a String" do
    @object.send(@method, nil).should == ""
  end

  it "converts a Float to a String" do
    @object.send(@method, 1.12).should == "1.12"
  end

  it "converts a boolean to a String" do
    @object.send(@method, false).should == "false"
    @object.send(@method, true).should == "true"
  end

  it "converts a constant to a String" do
    @object.send(@method, Object).should == "Object"
  end

  it "calls #to_s to convert an arbitrary object to a String" do
    obj = mock('test')
    obj.should_receive(:to_s).and_return("test")

    @object.send(@method, obj).should == "test"
  end

  it "raises a TypeError if #to_s does not exist" do
    obj = mock('to_s')
    obj.undefine(:to_s)

    lambda { @object.send(@method, obj) }.should raise_error(TypeError)
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if respond_to? returns false for #to_s" do
      obj = mock("to_s")
      obj.does_not_respond_to(:to_s)

      lambda { @object.send(@method, obj) }.should raise_error(TypeError)
    end
  end

  ruby_bug "#5158", "1.9.3.116" do
    it "raises a TypeError if respond_to? returns false for #to_s" do
      obj = mock("to_s")
      obj.does_not_respond_to(:to_s)

      lambda { @object.send(@method, obj) }.should raise_error(TypeError)
    end
  end

  ruby_version_is ""..."1.9" do
    it "raises a NoMethodError if #to_s is not defined but #respond_to?(:to_s) returns true" do
      # cannot use a mock because of how RSpec affects #method_missing
      obj = Object.new
      obj.undefine(:to_s)
      obj.responds_to(:to_s)

      lambda { @object.send(@method, obj) }.should raise_error(NoMethodError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a TypeError if #to_s is not defined, even though #respond_to?(:to_s) returns true" do
      # cannot use a mock because of how RSpec affects #method_missing
      obj = Object.new
      obj.undefine(:to_s)
      obj.responds_to(:to_s)

      lambda { @object.send(@method, obj) }.should raise_error(TypeError)
    end
  end

  it "calls #to_s if #respond_to?(:to_s) returns true" do
    obj = mock('to_s')
    obj.undefine(:to_s)
    obj.fake!(:to_s, "test")

    @object.send(@method, obj).should == "test"
  end

  it "raises a TypeError if #to_s does not return a String" do
    (obj = mock('123')).should_receive(:to_s).and_return(123)
    lambda { @object.send(@method, obj) }.should raise_error(TypeError)
  end

  it "returns the same object if it is already a String" do
    string = "Hello"
    string.should_not_receive(:to_s)
    string2 = @object.send(@method, string)
    string.should equal(string2)
  end

  it "returns the same object if it is an instance of a String subclass" do
    subklass = Class.new(String)
    string = subklass.new("Hello")
    string.should_not_receive(:to_s)
    string2 = @object.send(@method, string)
    string.should equal(string2)
  end
end

describe "Kernel.String" do
  it_behaves_like :kernel_String, :String, Kernel
end

describe "Kernel#String" do
  it_behaves_like :kernel_String, :String, Object.new

  it "is a private method" do
    Kernel.should have_private_instance_method(:String)
  end
end
