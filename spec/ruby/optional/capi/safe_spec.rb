require File.expand_path('../spec_helper', __FILE__)

load_extension("safe")

describe "CApiSafeSpecs" do
  before :each do
    @f = CApiSafeSpecs.new
  end

  not_compliant_on :rubinius do
    it "has a default safe level of 0" do
      @f.rb_safe_level.should == 0
    end

    it "throws an error when rb_secure is called with argument >= SAFE" do
      lambda { @f.rb_secure(0) }.should raise_error(SecurityError)
    end
  end
end
