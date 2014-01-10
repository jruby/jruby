require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#extend_object" do
  before :each do
    ScratchPad.clear
  end

  it "is called when #extend is called on an object" do
    ModuleSpecs::ExtendObject.should_receive(:extend_object)
    obj = mock("extended object")
    obj.extend ModuleSpecs::ExtendObject
  end

  it "extends the given object with its constants and methods by default" do
    obj = mock("extended direct")
    ModuleSpecs::ExtendObject.send :extend_object, obj

    obj.test_method.should == "hello test"
    obj.singleton_class.const_get(:C).should == :test
  end

  it "is called even when private" do
    obj = mock("extended private")
    obj.extend ModuleSpecs::ExtendObjectPrivate
    ScratchPad.recorded.should == :extended
  end
end
