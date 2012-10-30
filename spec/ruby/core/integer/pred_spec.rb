require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7" do
  describe "Integer#pred" do
    it "returns the Integer equal to self - 1" do
      0.pred.should eql(-1)
      -1.pred.should eql(-2)
      bignum_value.pred.should eql(bignum_value(-1))
      20.pred.should eql(19)
    end
  end
end
