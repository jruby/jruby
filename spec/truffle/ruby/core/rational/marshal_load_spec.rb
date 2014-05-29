require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Rational#marshal_load" do
    it "loads numerator and denominator" do
      r = Rational(1, 1)
      r.marshal_load([1, 2])
      r.should == Rational(1, 2)
    end

    it "loads instance variables" do
      a = [1, 2]
      a.instance_variable_set(:@ivar, :ivar)

      r = Rational(1, 1)
      r.marshal_load(a)
      r.instance_variable_get(:@ivar).should == :ivar
    end

    it "returns self" do
      r = Rational(1, 1)
      r.marshal_load([1, 2]).should equal(r)
    end
  end
end
