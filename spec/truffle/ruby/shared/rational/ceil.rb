require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe :rational_ceil, :shared => true do
    it "returns an Integer" do
      Rational(19.18).ceil.should be_kind_of(Integer)
    end

    it "returns the smallest integer >= self as an integer" do
      Rational(200, 87).ceil.should == 3
      Rational(6.1, 9).ceil.should == 1
      Rational(686543**99).ceil.should == 686543**99
    end
  end
end

ruby_version_is ""..."1.9" do

  require 'rational'

  describe :rational_ceil, :shared => true do
    it "returns an Integer" do
      Rational(19).ceil.should be_kind_of(Integer)
    end

    ruby_version_is ""..."1.8.6" do
      it "returns the smallest integer >= self as an integer" do
        Rational(200, 87).ceil.should == 3
        Rational(6, 9).ceil.should == 1
      end
    end

    ruby_version_is "1.8.6"..."" do
      it "returns the smallest integer >= self as an integer" do
        Rational(200, 87).ceil.should == 3
        Rational(6, 9).ceil.should == 1
        Rational(686543**99).ceil.should == 686543**99
      end
    end
  end
end
