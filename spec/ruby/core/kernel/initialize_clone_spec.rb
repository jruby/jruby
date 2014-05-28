require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#initialize_clone" do
  before do
    ScratchPad.clear
  end

  it "is a private method" do
    Kernel.should have_private_instance_method(:initialize_clone, false)
  end

  it "is called when object is cloned" do
    obj = KernelSpecs::Clone.new
    obj.clone
    ScratchPad.recorded.should == obj.object_id
  end

  it "calls initialize_copy by default" do
    obj = KernelSpecs::Duplicate.new(1, :a)
    other = KernelSpecs::Duplicate.new(2, :a)
    obj.send(:initialize_clone, other)
    ScratchPad.recorded.should == obj.object_id
  end
end
