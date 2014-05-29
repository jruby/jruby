require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Fixnum#odd?" do
    it "is false for zero" do
      0.odd?.should be_false
    end

    it "is false for even positive Fixnums" do
      4.odd?.should be_false
    end

    it "is false for even negative Fixnums" do
      (-4).odd?.should be_false
    end

    it "is true for odd positive Fixnums" do
      5.odd?.should be_true
    end

    it "is true for odd negative Fixnums" do
      (-5).odd?.should be_true
    end
  end
end
