require File.expand_path('../../../spec_helper', __FILE__)

describe "SystemStackError" do
  ruby_version_is ""..."1.9" do
    it "is a subclass of StandardError" do
      SystemStackError.superclass.should == StandardError
    end
  end

  ruby_version_is "1.9" do
    it "is a subclass of Exception" do
      SystemStackError.superclass.should == Exception
    end
  end
end

