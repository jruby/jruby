require File.expand_path('../../../spec_helper', __FILE__)

describe "Float#<=" do
  it "returns true if self is less than or equal to other" do
    (2.0 <= 3.14159).should == true
    (-2.7183 <= -24).should == false
    (0.0 <= 0.0).should == true
    (9_235.9 <= bignum_value).should == true
  end
end
