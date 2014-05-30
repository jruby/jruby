require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7" do
  describe "Bignum#even?" do
    it "returns true if self is even and positive" do
      (10000**10).even?.should be_true
    end

    it "returns true if self is even and negative" do
      (-10000**10).even?.should be_true
    end

    it "returns false if self is odd and positive" do
      (9879**976).even?.should be_false
    end

    it "returns false if self is odd and negative" do
      (-9879**976).even?.should be_false
    end
  end
end
