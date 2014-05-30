require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9.1" do
  describe "Precision#prec_i" do
    it "returns the same Integer when called on an Integer"  do
      1.prec_i.should == 1
    end

    it "converts Float to an Integer when called on an Integer" do
      1.9.prec_i.should == 1
    end
  end
end
