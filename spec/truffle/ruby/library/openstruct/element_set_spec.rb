require File.expand_path('../../../spec_helper', __FILE__)
require "ostruct"

describe "OpenStruct#[]=" do
  before :each do
    @os = OpenStruct.new
  end

  ruby_bug "redmine:4179", "1.9.2" do
    ruby_version_is ""..."2.0" do
      it "raises a NoMethodError" do
        lambda { @os[:foo] = 2 }.should raise_error(NoMethodError)
      end
    end
    ruby_version_is "2.0" do
      it "sets the associated value" do
        @os[:foo] = 42
        @os.foo.should == 42
      end
    end
  end
end
