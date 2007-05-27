require File.dirname(__FILE__) + '/../spec_helper'

context "Integer#bits" do
  specify "returns the minimum bits of storage needed for the number as a signed int" do
    1.bits.should == 2
    -1.bits.should == 2
    -2.bits.should == 2
    2.bits.should == 3
    4.bits.should == 4
    536870911.bits.should == 30
    -536870912.bits.should ==  30
    536870912.bits.should == 31
    -536870913.bits.should == 31
  end
end

