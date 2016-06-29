require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/singleton_method', __FILE__)

describe "BasicObject#singleton_method_undefined" do
  before :each do
    ScratchPad.clear
  end

  it "is a private method" do
    BasicObject.should have_private_instance_method(:singleton_method_undefined)
  end

  it "is called when a method is removed on self" do
    class << BasicObjectSpecs::SingletonMethod
      undef_method :singleton_method_to_undefine
    end
    ScratchPad.recorded.should == [:singleton_method_undefined, :singleton_method_to_undefine]
  end
end
