require File.expand_path('../../../spec_helper', __FILE__)

describe :rational_round, :shared => true do
  ruby_version_is "1.9" do
    it "rounds and integer to itself by default" do
      Rational(3).round.should == 3
    end

    it "returns an integer when it rounds to a whole integer" do
      Rational(35,10).round.should be_kind_of Fixnum
    end

    it "rounds a rational number to the nearest whole integer by default" do
      Rational(2,3).round.should == 1
    end

    it "rounds negative numbers away from zero" do
      Rational(-3,2).round.should == -2
    end

    it "rounds to the given precision" do
      Rational(123456, 1000).round(+1).should == Rational(1235, 10)
    end

    it "rounds to the given precision with a negative precision" do
      Rational(-123456, 1000).round(-2).should == Rational(-100, 1)
    end

    it "doesn't alter the value if the precision is too great" do
      Rational(-3,2).round(10).should == Rational(-3,2).round(20)
    end

    ruby_bug '#6605', '1.9.3' do
      it "doesn't fail when rounding to an absurdly large positive precision" do
        Rational(-3,2).round(2_097_171).should == Rational(-3,2)
      end
    end
  end
end
