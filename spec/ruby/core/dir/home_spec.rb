require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

describe "Dir.home" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it "returns the current user's home directory as a string if called without arguments" do
    Dir.home.should == home_directory
  end

  platform_is_not :windows do
    it "returns the named user's home directory as a string if called with an argument" do
      Dir.home(ENV['USER']).should == home_directory
    end
  end

  it "raises an ArgumentError if the named user doesn't exist" do
    lambda { Dir.home('geuw2n288dh2k') }.should raise_error(ArgumentError)
  end
end
