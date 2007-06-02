require File.dirname(__FILE__) + '/../spec_helper'

context "Fixnum instance method" do
  specify "size should return the number of bytes in the machine representation of self" do
    -1.size.should == 4
    0.size.should == 4
    4091.size.should == 4
  end
end
