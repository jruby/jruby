require File.expand_path('../spec_helper', __FILE__)

ruby_version_is "1.9" do
  load_extension("rational")

  describe :rb_Rational, :shared => true do
    it "creates a new Rational with numerator and denominator" do
      @r.send(@method, 1, 2).should == Rational(1, 2)
    end
  end

  describe "CApiRationalSpecs" do
    before :each do
      @r = CApiRationalSpecs.new
    end

    describe "rb_Rational" do
      it_behaves_like :rb_Rational, :rb_Rational
    end

    describe "rb_Rational2" do
      it_behaves_like :rb_Rational, :rb_Rational2
    end

    describe "rb_Rational1" do
      it "creates a new Rational with numerator and denominator of 1" do
        @r.rb_Rational1(5).should == Rational(5, 1)
      end
    end
  end
end
