require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#remove_instance_variable" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:remove_instance_variable)
  end

  it "removes an ivar of a given name and return it's value" do
    val = KernelSpecs::Ivar.new.send :remove_instance_variable, :@greeting
    val.should == "hello"
  end

  it "supports the name being a string" do
    val = KernelSpecs::Ivar.new.send :remove_instance_variable, "@greeting"
    val.should == "hello"
  end

  it "tries to call #to_str if it's not a String or Symbol" do
    s = mock("str")
    s.should_receive(:to_str).and_return("@greeting")

    val = KernelSpecs::Ivar.new.send :remove_instance_variable, s
    val.should == "hello"
  end

  it "raises NameError if the ivar isn't defined" do
    lambda {
      KernelSpecs::Ivar.new.send :remove_instance_variable, :@unknown
    }.should raise_error(NameError)
  end

  it "rejects unknown argument types" do
    lambda {
      KernelSpecs::Ivar.new.send :remove_instance_variable, Object
    }.should raise_error(TypeError)
  end
end
