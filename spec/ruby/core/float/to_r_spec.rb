require_relative '../../spec_helper'

describe "Float#to_r" do
  it "returns self as a Rational" do
    0.5.to_r.should.eql?(Rational(1, 2))
    -0.75.to_r.should.eql?(Rational(-3, 4))
    0.1.to_r.should.eql?(Rational(3602879701896397, 36028797018963968))
  end

  it "returns a Rational for a whole number" do
    0.0.to_r.should.eql?(Rational(0, 1))
    2.0.to_r.should.eql?(Rational(2, 1))
    -3.0.to_r.should.eql?(Rational(-3, 1))
  end

  it "returns a Rational for a Float of magnitude 2**52 or more" do
    (2.0 ** 52).to_r.should.eql?(Rational(4503599627370496, 1))
    (2.0 ** 64).to_r.should.eql?(Rational(18446744073709551616, 1))
    (-(2.0 ** 64)).to_r.should.eql?(Rational(-18446744073709551616, 1))
    1e300.to_r.should.eql?(Rational(1e300.to_i, 1))
  end

  it "raises a FloatDomainError for Infinity" do
    -> { infinity_value.to_r }.should.raise(FloatDomainError)
  end

  it "raises a FloatDomainError for NaN" do
    -> { nan_value.to_r }.should.raise(FloatDomainError)
  end
end
