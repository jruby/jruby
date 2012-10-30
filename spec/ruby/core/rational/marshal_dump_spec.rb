require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Rational#marshal_dump" do
    it "dumps numerator and denominator" do
      Rational(1, 2).marshal_dump.should == [1, 2]
    end

    it "dumps instance variables" do
      r = Rational(1, 2)
      r.instance_variable_set(:@ivar, :ivar)
      r.marshal_dump.instance_variable_get(:@ivar).should == :ivar
    end
  end
end
