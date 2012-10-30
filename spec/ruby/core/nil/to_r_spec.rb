require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "NilClass#to_r" do
    it "returns 0/1" do
      nil.to_r.should == Rational(0, 1)
    end
  end
end
