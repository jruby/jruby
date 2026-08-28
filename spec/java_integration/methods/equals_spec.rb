require File.dirname(__FILE__) + "/../spec_helper"

describe "A class that implements Comparable" do
  it "still uses .equals for ==" do
    # JRUBY-6967
    expect(java.util.Date.new.==('foo')).to eq(false)
  end
end

describe "A class that defines an isEqual method" do
  it "does not override JavaProxy#equal?" do
    a = Java::java_integration.fixtures.IsEqualClass.new

    # a.class.instance_method(:equal?).should == JavaProxy.instance_method(:equal?)
    JRuby.ref(a.class).searchMethod("equal?").should == JRuby.ref(JavaProxy).searchMethod("equal?")

    a.equal?(a).should be_truthy
    a.getCalled.should be_falsy

    a.isEqual(a).should be_truthy
    a.getCalled.should be_truthy
  end
end