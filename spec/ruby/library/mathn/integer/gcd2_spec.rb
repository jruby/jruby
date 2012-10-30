require File.expand_path('../../../../spec_helper', __FILE__)
require 'mathn'

ruby_version_is ''...'1.9' do
  describe "Integer#gcd2" do
    it "Returns the greatest common divisor of the two numbers" do
      15.gcd2(5).should == 5
      15.gcd2(-6).should == 3
      -23.gcd2(19).should == 1
      -10.gcd2(-2).should == 2
    end

    it "raises a ZeroDivisionError when is called on zero" do
      lambda { 0.gcd2(2) }.should raise_error(ZeroDivisionError)
      lambda { 2.gcd2(0) }.should raise_error(ZeroDivisionError)
    end
  end
end
