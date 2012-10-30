require File.expand_path('../../../spec_helper', __FILE__)

describe "ENV.invert" do

  it "returns a hash with ENV.keys as the values and vice versa" do
    ENV["foo"] = "bar"
    ENV.delete "bar"
    ENV.invert["bar"].should == "foo"
    ENV["foo"].should == "bar"
    ENV["bar"].should == nil
  end
end
