require File.expand_path('../../../spec_helper', __FILE__)
require 'rational'

describe :rational_exponent, :shared => true do
  describe "when passed Rational" do
    conflicts_with :Prime do
      ruby_version_is ""..."1.9" do
        it "converts self to a Float and returns it raised to the passed argument" do
          (Rational(3, 4) ** Rational(4, 3)).should be_close(0.681420222312052, TOLERANCE)
          (Rational(3, 4) ** Rational(-4, 3)).should be_close(1.46752322173095, TOLERANCE)
          (Rational(3, 4) ** Rational(4, -3)).should be_close(1.46752322173095, TOLERANCE)

          (Rational(3, 4) ** Rational(0, 3)).should eql(1.0)
          (Rational(-3, 4) ** Rational(0, 3)).should eql(1.0)
          (Rational(3, -4) ** Rational(0, 3)).should eql(1.0)
          (Rational(3, 4) ** Rational(0, -3)).should eql(1.0)

          (Rational(bignum_value, 4) ** Rational(0, 3)).should eql(1.0)
          (Rational(3, -bignum_value) ** Rational(0, 3)).should eql(1.0)
          (Rational(3, 4) ** Rational(0, bignum_value)).should eql(1.0)
          (Rational(3, 4) ** Rational(0, -bignum_value)).should eql(1.0)
        end
      end

      ruby_version_is "1.9" do
        it "returns Rational(1) if the exponent is Rational(0)" do
          (Rational(0) ** Rational(0)).should eql(Rational(1))
          (Rational(1) ** Rational(0)).should eql(Rational(1))
          (Rational(3, 4) ** Rational(0)).should eql(Rational(1))
          (Rational(-1) ** Rational(0)).should eql(Rational(1))
          (Rational(-3, 4) ** Rational(0)).should eql(Rational(1))
          (Rational(bignum_value) ** Rational(0)).should eql(Rational(1))
          (Rational(-bignum_value) ** Rational(0)).should eql(Rational(1))
        end

        it "returns self raised to the argument as a Rational if the exponent's denominator is 1" do
          (Rational(3, 4) ** Rational(1, 1)).should eql(Rational(3, 4))
          (Rational(3, 4) ** Rational(2, 1)).should eql(Rational(9, 16))
          (Rational(3, 4) ** Rational(-1, 1)).should eql(Rational(4, 3))
          (Rational(3, 4) ** Rational(-2, 1)).should eql(Rational(16, 9))
        end

        it "returns self raised to the argument as a Float if the exponent's denominator is not 1" do
          (Rational(3, 4) ** Rational(4, 3)).should be_close(0.681420222312052, TOLERANCE)
          (Rational(3, 4) ** Rational(-4, 3)).should be_close(1.46752322173095, TOLERANCE)
          (Rational(3, 4) ** Rational(4, -3)).should be_close(1.46752322173095, TOLERANCE)
        end

        it "returns a complex number when self is negative and the passed argument is not 0" do
          (Rational(-3, 4) ** Rational(-4, 3)).should == Complex(
            -0.7337616108654732, 1.2709123906625817)
        end
      end

      ruby_version_is ""..."1.9" do
        it "returns NaN when self is negative and the passed argument is not 0" do
          (Rational(-3, 4) ** Rational(-4, 3)).nan?.should be_true
        end
      end
    end
  end

  describe "when passed Integer" do
    it "returns the Rational value of self raised to the passed argument" do
      (Rational(3, 4) ** 4).should == Rational(81, 256)
      (Rational(3, 4) ** -4).should == Rational(256, 81)
      (Rational(-3, 4) ** -4).should == Rational(256, 81)
      (Rational(3, -4) ** -4).should == Rational(256, 81)

      (Rational(bignum_value, 4) ** 4).should == Rational(28269553036454149273332760011886696253239742350009903329945699220681916416, 1)
      (Rational(3, bignum_value) ** -4).should == Rational(7237005577332262213973186563042994240829374041602535252466099000494570602496, 81)
      (Rational(-bignum_value, 4) ** -4).should == Rational(1, 28269553036454149273332760011886696253239742350009903329945699220681916416)
      (Rational(3, -bignum_value) ** -4).should == Rational(7237005577332262213973186563042994240829374041602535252466099000494570602496, 81)
    end

    conflicts_with :Prime do
      it "returns Rational(1, 1) when the passed argument is 0" do
        (Rational(3, 4) ** 0).should eql(Rational(1, 1))
        (Rational(-3, 4) ** 0).should eql(Rational(1, 1))
        (Rational(3, -4) ** 0).should eql(Rational(1, 1))

        (Rational(bignum_value, 4) ** 0).should eql(Rational(1, 1))
        (Rational(3, -bignum_value) ** 0).should eql(Rational(1, 1))
      end
    end
  end

  describe "when passed Bignum" do
    ruby_version_is ""..."1.9" do
      it "returns Rational(0) when self is Rational(0) and the exponent is positive" do
        (Rational(0) ** bignum_value).should eql(Rational(0))
      end

      it "returns Rational(1, 0) when self is Rational(0) and the exponent is negative" do
        result = (Rational(0) ** -bignum_value)
        result.numerator.should eql(1)
        result.denominator.should eql(0)
      end

      it "returns Rational(1) when self is Rational(1)" do
        (Rational(1) ** bignum_value).should eql(Rational(1))
      end

      it "returns Rational(1) when self is Rational(-1) and the exponent is even" do
        (Rational(-1) ** bignum_value(0)).should eql(Rational(1))
      end

      it "returns Rational(-1) when self is Rational(-1) and the exponent is odd" do
        (Rational(-1) ** bignum_value(1)).should eql(Rational(-1))
      end

      it "raises FloatDomainError when self is > 1 or < -1" do
        lambda { Rational(2) ** bignum_value           }.should raise_error(FloatDomainError)
        lambda { Rational(-2) ** bignum_value          }.should raise_error(FloatDomainError)
        lambda { Rational(fixnum_max) ** bignum_value  }.should raise_error(FloatDomainError)
        lambda { Rational(fixnum_min) ** bignum_value  }.should raise_error(FloatDomainError)

        lambda { Rational(2) ** -bignum_value          }.should raise_error(FloatDomainError)
        lambda { Rational(-2) ** -bignum_value         }.should raise_error(FloatDomainError)
        lambda { Rational(fixnum_max) ** -bignum_value }.should raise_error(FloatDomainError)
        lambda { Rational(fixnum_min) ** -bignum_value }.should raise_error(FloatDomainError)
      end
    end

    ruby_version_is "1.9" do
      ruby_bug "#5713", "2.0" do
        it "returns Rational(0) when self is Rational(0) and the exponent is positive" do
          (Rational(0) ** bignum_value).should eql(Rational(0))
        end

        it "raises ZeroDivisionError when self is Rational(0) and the exponent is negative" do
          lambda { Rational(0) ** -bignum_value }.should raise_error(ZeroDivisionError)
        end

        it "returns Rational(1) when self is Rational(1)" do
          (Rational(1) **  bignum_value).should eql(Rational(1))
          (Rational(1) ** -bignum_value).should eql(Rational(1))
        end

        it "returns Rational(1) when self is Rational(-1) and the exponent is positive and even" do
          (Rational(-1) ** bignum_value(0)).should eql(Rational(1))
          (Rational(-1) ** bignum_value(2)).should eql(Rational(1))
        end

        it "returns Rational(-1) when self is Rational(-1) and the exponent is positive and odd" do
          (Rational(-1) ** bignum_value(1)).should eql(Rational(-1))
          (Rational(-1) ** bignum_value(3)).should eql(Rational(-1))
        end
      end

      it "returns positive Infinity when self is > 1" do
        (Rational(2) ** bignum_value).infinite?.should == 1
        (Rational(fixnum_max) ** bignum_value).infinite?.should == 1
      end

      it "returns 0.0 when self is > 1 and the exponent is negative" do
        (Rational(2) ** -bignum_value).should eql(0.0)
        (Rational(fixnum_max) ** -bignum_value).should eql(0.0)
      end

      # Fails on linux due to pow() bugs in glibc: http://sources.redhat.com/bugzilla/show_bug.cgi?id=3866
      platform_is_not :linux do
        it "returns positive Infinity when self < -1" do
          (Rational(-2) ** bignum_value).infinite?.should == 1
          (Rational(-2) ** (bignum_value + 1)).infinite?.should == 1
          (Rational(fixnum_min) ** bignum_value).infinite?.should == 1
        end

        it "returns 0.0 when self is < -1 and the exponent is negative" do
          (Rational(-2) ** -bignum_value).should eql(0.0)
          (Rational(fixnum_min) ** -bignum_value).should eql(0.0)
        end
      end
    end
  end

  describe "when passed Float" do
    it "returns self converted to Float and raised to the passed argument" do
      (Rational(3, 1) ** 3.0).should eql(27.0)
      (Rational(3, 1) ** 1.5).should be_close(5.19615242270663, TOLERANCE)
      (Rational(3, 1) ** -1.5).should be_close(0.192450089729875, TOLERANCE)
    end

    ruby_version_is ""..."1.9" do
      it "returns NaN if self is negative and the passed argument is not 0" do
        (Rational(-3, 2) ** 1.5).nan?.should be_true
        (Rational(3, -2) ** 1.5).nan?.should be_true
        (Rational(3, -2) ** -1.5).nan?.should be_true
      end

      it "returns 1.0 when the passed argument is 0.0" do
        (Rational(3, 4) ** 0.0).should eql(1.0)
        (Rational(-3, 4) ** 0.0).should eql(1.0)
        (Rational(-3, 4) ** 0.0).should eql(1.0)
      end
    end

    ruby_version_is "1.9" do
      it "returns a complex number if self is negative and the passed argument is not 0" do
        (Rational(-3, 2) ** 1.5).should be_close(                                                Complex(
                                                                                                   -3.374618290464398e-16, -1.8371173070873836), TOLERANCE)
        (Rational(3, -2) ** 1.5).should be_close(                                                Complex(
                                                                                                   -3.374618290464398e-16, -1.8371173070873836), TOLERANCE)
        (Rational(3, -2) ** -1.5).should be_close(                                               Complex(
                                                                                                   -9.998869008783402e-17, 0.5443310539518174), TOLERANCE)
      end

      it "returns Complex(1.0) when the passed argument is 0.0" do
        (Rational(3, 4) ** 0.0).should == Complex(1.0)
        (Rational(-3, 4) ** 0.0).should == Complex(1.0)
        (Rational(-3, 4) ** 0.0).should == Complex(1.0)
      end
    end
  end

  it "calls #coerce on the passed argument with self" do
    rational = Rational(3, 4)
    obj      = mock("Object")
    obj.should_receive(:coerce).with(rational).and_return([1, 2])

    rational ** obj
  end

  it "calls #** on the coerced Rational with the coerced Object" do
    rational = Rational(3, 4)

    coerced_rational = mock("Coerced Rational")
    coerced_rational.should_receive(:**).and_return(:result)

    coerced_obj = mock("Coerced Object")

    obj = mock("Object")
    obj.should_receive(:coerce).and_return([coerced_rational, coerced_obj])

    (rational ** obj).should == :result
  end

  ruby_version_is ""..."1.9" do
    it "returns Rational(1, 0) for Rational(0, 1) passed a negative Integer" do
      [-1, -4, -9999].each do |exponent|
        result = (Rational(0, 1) ** exponent)
        result.numerator.should eql(1)
        result.denominator.should eql(0)
      end
    end

    conflicts_with :Prime do
      it "returns Infinity for Rational(0, 1) passed a negative Rational" do
        [Rational(-1, 1), Rational(-3, 1), Rational(-3, 2)].each do |exponent|
          (Rational(0, 1) ** exponent).infinite?.should == 1
        end
      end
    end
  end

  ruby_version_is "1.9" do
    it "raises ZeroDivisionError for Rational(0, 1) passed a negative Integer" do
      [-1, -4, -9999].each do |exponent|
        lambda { Rational(0, 1) ** exponent }.should raise_error(ZeroDivisionError, "divided by 0")
      end
    end

    it "raises ZeroDivisionError for Rational(0, 1) passed a negative Rational with denominator 1" do
      [Rational(-1, 1), Rational(-3, 1)].each do |exponent|
        lambda { Rational(0, 1) ** exponent }.should raise_error(ZeroDivisionError, "divided by 0")
      end
    end

    ruby_bug "#7513", "2.0.0" do
      it "raises ZeroDivisionError for Rational(0, 1) passed a negative Rational" do
        lambda { Rational(0, 1) ** Rational(-3, 2) }.should raise_error(ZeroDivisionError, "divided by 0")
      end
    end
  end

  it "returns Infinity for Rational(0, 1) passed a negative Float" do
    [-1.0, -3.0, -3.14].each do |exponent|
      (Rational(0, 1) ** exponent).infinite?.should == 1
    end
  end
end
