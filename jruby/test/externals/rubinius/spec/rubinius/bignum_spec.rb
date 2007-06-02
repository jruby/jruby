require File.dirname(__FILE__) + '/../spec_helper'

context "Rubinius Bignum" do
  specify "should have max value 2 ** 29" do
    max = 2 ** 29
    max.class.should == Bignum
    (max - 1).class.should == Fixnum
  end
end
