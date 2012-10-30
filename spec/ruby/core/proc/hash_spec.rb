require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Proc#hash" do
    it "is provided" do
      proc {}.respond_to?(:hash).should be_true
      lambda {}.respond_to?(:hash).should be_true
    end

    it "returns an Integer" do
      proc { 1 + 489 }.hash.should be_kind_of(Integer)
    end

    it "is stable" do
      body = proc { :foo }
      (proc &body).hash.should == (proc &body).hash
    end

    it "does not depend on whether self is a proc or lambda" do
      body = proc { :foo }
      (proc &body).hash.should == (lambda &body).hash
    end
  end
end
