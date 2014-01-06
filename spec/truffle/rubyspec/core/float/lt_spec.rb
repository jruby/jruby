require File.expand_path('../../../spec_helper', __FILE__)

describe "Float#<" do
  it "returns true if self is less than other" do
    (71.3 < 91.8).should == true
    (192.6 < -500).should == false
    (-0.12 < 0x4fffffff).should == true
  end
end
