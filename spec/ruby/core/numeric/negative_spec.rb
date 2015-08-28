require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "2.3" do
  describe "Numeric#negative?" do
    describe "on positive numbers" do
      it "returns false" do
        1.negative?.should be_false
        0.1.negative?.should be_false
      end
    end
    
    describe "on zero" do
      it "returns false" do
        0.negative?.should be_false
        0.0.negative?.should be_false
      end
    end
    
    describe "on negative numbers" do
      it "returns true" do
        -1.negative?.should be_true
        -0.1.negative?.should be_true
      end
    end
  end
end
