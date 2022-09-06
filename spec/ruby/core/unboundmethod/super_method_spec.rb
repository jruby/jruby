require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative '../method/fixtures/classes'

describe "UnboundMethod#super_method" do
  it "returns the method that would be called by super in the method" do
    meth = UnboundMethodSpecs::C.instance_method(:overridden)
    meth = meth.super_method
    meth.should == UnboundMethodSpecs::B.instance_method(:overridden)
    meth = meth.super_method
    meth.should == UnboundMethodSpecs::A.instance_method(:overridden)
  end

  it "returns nil when there's no super method in the parent" do
    method = Kernel.instance_method(:method)
    method.super_method.should == nil
  end

  it "returns nil when the parent's method is removed" do
    parent = Class.new { def foo; end }
    child = Class.new(parent) { def foo; end }

    method = child.instance_method(:foo)

    parent.send(:undef_method, :foo)

    method.super_method.should == nil
  end

  # jruby:7240
  context "after changing an inherited methods visiblity" do
    it "returns the expected super_method" do
      MethodSpecs::ChangedVisibility::C.send :public, :derp

      method = MethodSpecs::ChangedVisibility::C.instance_method(:derp)
      method.super_method.owner.should == MethodSpecs::ChangedVisibility::A
    end
  end
end
