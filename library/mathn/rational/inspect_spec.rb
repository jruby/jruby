require File.expand_path('../../../../spec_helper', __FILE__)
require 'mathn'

describe "Rational#inspect" do
  ruby_version_is ''...'1.9' do
    it "returns a string representation of self" do
      Rational.new!(3, 4).inspect.should == "3/4"
      Rational.new!(-5, 8).inspect.should == "-5/8"
      Rational.new!(-1, -2).inspect.should == "1/2"
      Rational.new!(0, 2).inspect.should == "0/2"
      Rational.new!(bignum_value, 1).inspect.should == "#{bignum_value}/1"
    end
  end

  ruby_version_is '1.9' do
    it "returns a string representation of self" do
      Rational(3, 4).inspect.should == "(3/4)"
      Rational(-5, 8).inspect.should == "(-5/8)"
      Rational(-1, -2).inspect.should == "(1/2)"
      Rational(0, 2).inspect.should == "0"
      Rational(bignum_value, 1).inspect.should == "#{bignum_value}"
    end
  end
end
