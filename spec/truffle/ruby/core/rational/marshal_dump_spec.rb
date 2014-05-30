require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9"..."2.0" do
  describe "Rational#marshal_dump" do
    it "dumps numerator and denominator" do
      Rational(1, 2).marshal_dump.should == [1, 2]
    end
  end
end
