require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "NilClass#to_c" do
    it "returns Complex(0, 0)" do
      nil.to_c.should eql(Complex(0, 0))
    end
  end
end
