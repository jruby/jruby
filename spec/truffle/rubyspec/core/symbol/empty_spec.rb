require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Symbol#empty?" do
    it "returns true if self is empty" do
      # 1.8.6 chokes on empty Symbol literals
      "".to_sym.empty?.should be_true
    end

    it "returns false if self is non-empty" do
      :"a".empty?.should be_false
    end
  end
end
