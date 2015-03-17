require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'
  require "rational"

  describe "Complex.generic?" do
    it "returns true when given an Integer, Float or Rational" do
      Complex.generic?(1).should == true
      Complex.generic?(-1).should == true

      Complex.generic?(20.3).should == true
      Complex.generic?(-20.3).should == true

      Complex.generic?(bignum_value).should == true
      Complex.generic?(-bignum_value).should == true

      Complex.generic?(Rational(3, 4)).should == true
      Complex.generic?(-Rational(3, 4)).should == true

      Complex.generic?(:symbol).should == false
      Complex.generic?("string").should == false
      Complex.generic?(mock("Object")).should == false
    end
  end
end
