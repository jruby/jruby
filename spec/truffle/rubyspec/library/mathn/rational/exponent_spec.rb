require File.expand_path('../../../../spec_helper', __FILE__)
require 'mathn'

ruby_version_is ''...'1.9' do
  describe "Rational#** when passed [Rational]" do
    it "returns Rational.new!(1, 1) when the passed argument is 0" do
      (Rational.new!(3, 4) ** Rational.new!(0, 3)).should == Rational.new!(1,1)
      (Rational.new!(-3, 4) ** Rational.new!(0, 3)).should == Rational.new!(1,1)
      (Rational.new!(3, -4) ** Rational.new!(0, 3)).should == Rational.new!(1,1)
      (Rational.new!(3, 4) ** Rational.new!(0, -3)).should == Rational.new!(1,1)
    end

    it "returns Rational.new!(1, 1) when self is 1" do
      (Rational.new!(1,1) ** Rational.new!(2, 3)).should == Rational.new!(1,1)
      (Rational.new!(1,1) ** Rational.new!(-2, 3)).should == Rational.new!(1,1)
      (Rational.new!(1,1) ** Rational.new!(2, -3)).should == Rational.new!(1,1)
      (Rational.new!(1,1) ** Rational.new!(-2, -3)).should == Rational.new!(1,1)
    end

    it "returns Rational.new!(0, 1) when self is 0" do
      (Rational.new!(0,1) ** Rational.new!(2, 3)).should == Rational.new!(0,1)
      (Rational.new!(0,1) ** Rational.new!(-2, 3)).should == Rational.new!(0,1)
      (Rational.new!(0,1) ** Rational.new!(2, -3)).should == Rational.new!(0,1)
      (Rational.new!(0,1) ** Rational.new!(-2, -3)).should == Rational.new!(0,1)
    end

    it "returns a Complex number when self is negative" do
      (Rational.new!(-1,2) ** Rational.new!(2, 3)).should be_close(Complex(-0.314980262473718, 0.545561817985861), TOLERANCE)
      (Rational.new!(-1,2) ** Rational.new!(-2, 3)).should be_close(Complex(-0.793700525984099, -1.3747296369986), TOLERANCE)
      (Rational.new!(-1,2) ** Rational.new!(2, -3)).should be_close(Complex(-0.793700525984099, -1.3747296369986), TOLERANCE)
    end
  end

  describe "Rational#** when passed [Integer]" do
    it "returns the Rational value of self raised to the passed argument" do
      (Rational.new!(3, 4) ** 4).should == Rational.new!(81, 256)
      (Rational.new!(3, 4) ** -4).should == Rational.new!(256, 81)
      (Rational.new!(-3, 4) ** -4).should == Rational.new!(256, 81)
      (Rational.new!(3, -4) ** -4).should == Rational.new!(256, 81)
    end

    it "returns Rational.new!(1, 1) when the passed argument is 0" do
      (Rational.new!(3, 4) ** 0).should == Rational.new!(1, 1)
      (Rational.new!(-3, 4) ** 0).should == Rational.new!(1, 1)
      (Rational.new!(3, -4) ** 0).should == Rational.new!(1, 1)

      (Rational.new!(bignum_value, 4) ** 0).should == Rational.new!(1, 1)
      (Rational.new!(3, -bignum_value) ** 0).should == Rational.new!(1, 1)
    end
  end

  describe "Rational#** when passed [Float]" do
    it "returns self converted to Float and raised to the passed argument" do
      (Rational.new!(3, 1) ** 3.0).should == 27.0
      (Rational.new!(3, 1) ** 1.5).should be_close(5.19615242270663, TOLERANCE)
      (Rational.new!(3, 1) ** -1.5).should be_close(0.192450089729875, TOLERANCE)
    end

    it "returns 1.0 when the passed argument is 0" do
      (Rational.new!(3, 4) ** 0.0).should == 1.0
      (Rational.new!(-3, 4) ** 0.0).should == 1.0
      (Rational.new!(-3, 4) ** 0.0).should == 1.0
    end

    it "returns NaN if self is negative and the passed argument is not 0" do
      (Rational.new!(-3, 2) ** 1.5).nan?.should be_true
      (Rational.new!(3, -2) ** 1.5).nan?.should be_true
      (Rational.new!(3, -2) ** -1.5).nan?.should be_true
    end
  end
end
