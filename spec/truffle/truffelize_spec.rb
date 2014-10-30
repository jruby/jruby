require "truffelize"

# Note that we pollute some core classes, modules and objects here. This is
# because we need receivers to be simple objects. In the future when we can pass
# arbitrary objects to Truffle and back this won't be a problem.

def truffelized_top_level_method
  truffelized?
end

truffelize :truffelized_top_level_method

class NilClass

  def nil_class_truffelized_test_method
    truffelized?
  end

  truffelize :nil_class_truffelized_test_method

end

top_level_self = self

module Kernel

  def kernel_module_truffelized_test_method
    truffelized?
  end

  truffelize :kernel_module_truffelized_test_method

  def non_truffelized_test_method
    truffelized?
  end

  def truffelized_test_method
    truffelized?
  end

  truffelize :truffelized_test_method

  def truffle_interface(a)
    a
  end

  truffelize :truffle_interface

end

describe "The truffelize method" do

  it "works at the top level" do
    nil.instance_eval do
      truffelized_top_level_method.should == true
    end
  end

  it "works on a class instance method" do
    nil.nil_class_truffelized_test_method.should == true
  end

  it "works on a module method" do
    Kernel.kernel_module_truffelized_test_method.should == true
  end

end

describe "The truffelized? method" do

  it "returns false when outside Truffle" do
    Kernel.non_truffelized_test_method.should == false
  end
  
  it "returns true when inside Truffle" do
    Kernel.truffelized_test_method.should == true
  end
end

describe "The Truffle interface" do

  it "passes top-level self" do
    Kernel.truffle_interface(top_level_self).should == top_level_self
  end

  it "passes NilClass" do
    Kernel.truffle_interface(nil).should == nil
  end

  it "passes TrueClass" do
    Kernel.truffle_interface(true).should == true
  end

  it "passes FalseClass" do
    Kernel.truffle_interface(false).should == false
  end

  it "passes Fixnum" do
    Kernel.truffle_interface(14).should == 14
  end

  it "passes Float" do
    Kernel.truffle_interface(14.5).should be_within(0.0001).of(14.5)
  end

end
