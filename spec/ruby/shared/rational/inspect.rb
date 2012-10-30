require File.expand_path('../../../spec_helper', __FILE__)
require 'rational'

describe :rational_inspect, :shared => true do
  conflicts_with :Prime do
    ruby_version_is ""..."1.9" do
      it "returns a reconstructable string representation of self" do
        Rational(3, 4).inspect.should == "Rational(3, 4)"
        Rational(-5, 8).inspect.should == "Rational(-5, 8)"
        Rational(-1, -2).inspect.should == "Rational(1, 2)"
        Rational(bignum_value, 1).inspect.should == "Rational(#{bignum_value}, 1)"
      end
    end

    ruby_version_is "1.9" do
      it "returns a string representation of self" do
        Rational(3, 4).inspect.should == "(3/4)"
        Rational(-5, 8).inspect.should == "(-5/8)"
        Rational(-1, -2).inspect.should == "(1/2)"
        Rational(bignum_value, 1).inspect.should == "(#{bignum_value}/1)"
      end
    end
  end
end
