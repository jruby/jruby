require File.expand_path('../../../spec_helper', __FILE__)

describe "Integer#integer?" do
  it "returns true" do
    0.integer?.should == true
    -1.integer?.should == true
    bignum_value.integer?.should == true
  end
end
