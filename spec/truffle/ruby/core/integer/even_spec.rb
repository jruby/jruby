require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7" do
  describe "Integer#even?" do
    it "returns true when self is an even number" do
      (-2).even?.should be_true
      (-1).even?.should be_false

      0.even?.should be_true
      1.even?.should be_false
      2.even?.should be_true

      bignum_value(0).even?.should be_true
      bignum_value(1).even?.should be_false

      (-bignum_value(0)).even?.should be_true
      (-bignum_value(1)).even?.should be_false
    end
  end
end
