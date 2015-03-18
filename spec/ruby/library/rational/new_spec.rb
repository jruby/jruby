ruby_version_is ""..."1.9" do
  describe "Rational.new" do
    it "is private" do
      Rational.should have_private_method(:new)
    end
  end

  describe "Rational.new!" do
    it "returns a Rational with the passed numerator and denominator not reduced to their lowest terms" do
      Rational.new!(2, 4).should_not eql(Rational(1, 4))
      Rational.new!(6, 8).should_not eql(Rational(3, 4))
      Rational.new!(-5, 10).should_not eql(Rational(-1, 2))
    end

    it "does not check for divisions by 0" do
      lambda { Rational.new!(4, 0) }.should_not raise_error(ZeroDivisionError)
      lambda { Rational.new!(0, 0) }.should_not raise_error(ZeroDivisionError)
    end
  end
end
