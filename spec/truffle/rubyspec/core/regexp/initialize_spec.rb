require File.expand_path('../../../spec_helper', __FILE__)

describe "Regexp#initialize" do
  it "is a private method" do
    Regexp.should have_private_method(:initialize)
  end

  it "raises a SecurityError on a Regexp literal" do
    lambda { //.send(:initialize, "") }.should raise_error(SecurityError)
  end

  ruby_version_is ""..."1.9" do
    it "reinitializes an initialized non-literal Regexp" do
      r = Regexp.new("a")
      r.send(:initialize, "b")
      r.should == /b/
    end
  end

  ruby_version_is "1.9" do
    it "raises a TypeError on an initialized non-literal Regexp" do
      lambda { Regexp.new("").send(:initialize, "") }.should raise_error(TypeError)
    end
  end
end
