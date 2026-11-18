require_relative '../../spec_helper'

describe "Regexp#initialize" do
  it "is a private method" do
    Regexp.should have_private_instance_method(:initialize)
  end

  it "raises a FrozenError on a Regexp literal" do
    -> { //.send(:initialize, "") }.should raise_error(FrozenError)
  end

  it "raises a TypeError on an initialized non-literal Regexp" do
    -> { Regexp.new("").send(:initialize, "") }.should raise_error(TypeError)
  end

  it "does not change the options or timeout of an initialized Regexp subclass" do
    r = Class.new(Regexp).new("a")
    -> { r.send(:initialize, "b", Regexp::IGNORECASE, timeout: 3) }.should raise_error(TypeError)
    r.options.should == 0
    r.timeout.should == nil
  end
end
