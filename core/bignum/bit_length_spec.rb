require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "2.1" do
  describe "Bignum#bit_length" do
    it "returns the position of the leftmost bit of a positive number" do
      (1 << 100).bit_length.should == 101
      (1 << 100).succ.bit_length.should == 101
      (1 << 100).pred.bit_length.should == 100
      (1 << 10000).bit_length.should == 10001
    end

    it "returns the position of the leftmost 0 bit of a negative number" do
      ((-1 << 100)-1).bit_length.should == 101
      ((-1 << 100)-1).succ.bit_length.should == 100
      ((-1 << 100)-1).pred.bit_length.should == 101
      ((-1 << 10000)-1).bit_length.should == 10001
    end
  end
end
