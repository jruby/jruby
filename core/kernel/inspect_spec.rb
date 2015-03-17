require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#inspect" do
  it "returns a String" do
    Object.new.inspect.should be_an_instance_of(String)
  end

  it "returns a tainted string if self is tainted" do
    Object.new.taint.inspect.tainted?.should be_true
  end

  it "returns an untrusted string if self is untrusted" do
    Object.new.untrust.inspect.untrusted?.should be_true
  end
end
