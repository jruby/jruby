require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "UnboundMethod#super_method" do
  ruby_version_is "2.2" do
    it "returns the method that would be called by super in the method" do
      meth = UnboundMethodSpecs::C.instance_method(:overridden)
      meth = meth.super_method
      meth.should == UnboundMethodSpecs::B.instance_method(:overridden)
      meth = meth.super_method
      meth.should == UnboundMethodSpecs::A.instance_method(:overridden)
    end

    it "returns nil when there's no super method in the parent" do
      method = Object.instance_method(:method)
      method.super_method.should == nil
    end

    it "returns nil when the parent's method is removed" do
      object = UnboundMethodSpecs::B
      method = object.instance_method(:overridden)

      UnboundMethodSpecs::A.class_eval { undef :overridden }

      method.super_method.should == nil
    end
  end
end
