require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/singleton_method', __FILE__)

describe "BasicObject#singleton_method_removed" do
  before :each do
    ScratchPad.clear
  end

  it "is a private method" do
    BasicObject.should have_private_instance_method(:singleton_method_removed)
  end

  it "is called when a method is removed on self" do
    class << BasicObjectSpecs::SingletonMethod
      remove_method :singleton_method_to_remove
    end
    ScratchPad.recorded.should == [:singleton_method_removed, :singleton_method_to_remove]
  end
end
