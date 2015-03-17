require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../fixtures/kernel/singleton_method', __FILE__)

describe :singleton_method_removed, :shared => true do
  it "is a private method" do
    @object.should have_private_instance_method(@method)
  end

  before :each do
    ScratchPad.clear
  end

  it "is called when a method is removed on self" do
    class << KernelSpecs::SingletonMethod
      remove_method :singleton_method_to_remove
    end
    ScratchPad.recorded.should == [:method_removed, :singleton_method_to_remove]
  end

end
