require File.dirname(__FILE__) + '/../spec_helper'

context "Fixnum instance method" do
  specify "coerce should return [Bignum, Bignum] if other is a Bignum" do
    a = 1.coerce(0xffffffff)
    a.should == [4294967295, 1]
    a.collect { |i| i.class }.should == [Bignum, Bignum]
  end
end
