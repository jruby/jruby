require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../fixtures/kernel/singleton_method', __FILE__)

describe :singleton_method_undefined, :shared => true do
  before :each do
    ScratchPad.clear
  end

  it "is a private method" do
    @object.should have_private_instance_method(@method)
  end

  it "is called when a method is removed on self" do
    class << KernelSpecs::SingletonMethod
      undef_method :singleton_method_to_undefine
    end
    ScratchPad.recorded.should == [:method_undefined, :singleton_method_to_undefine]
  end
end
