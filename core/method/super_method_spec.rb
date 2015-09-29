require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Method#super_method" do
  ruby_version_is "2.2" do
    it "returns the method that would be called by super in the method" do
      obj = MethodSpecs::C.new
      obj.extend MethodSpecs::OverrideAgain
      meth = obj.method(:overridden)

      s_meth = meth.super_method
      s_meth.owner.should == MethodSpecs::C
      s_meth.receiver.should == obj
      s_meth.name.should == :overridden

      ss_meth = meth.super_method.super_method
      ss_meth.owner.should == MethodSpecs::BetweenBAndC
      ss_meth.receiver.should == obj
      ss_meth.name.should == :overridden

      sss_meth = meth.super_method.super_method.super_method
      sss_meth.owner.should == MethodSpecs::B
      sss_meth.receiver.should == obj
      sss_meth.name.should == :overridden
    end

    it "returns nil when there's no super method in the parent" do
      method = Object.new.method(:method)
      method.super_method.should == nil
    end

    it "returns nil when the parent's method is removed" do
      object = MethodSpecs::B.new
      method = object.method(:overridden)

      MethodSpecs::A.class_eval { undef :overridden }

      method.super_method.should == nil
    end
  end
end
