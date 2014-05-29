require File.expand_path('../../../spec_helper', __FILE__)

describe "Class#initialize" do
  it "is private" do
    Class.should have_private_method(:initialize)
  end

  it "raises a TypeError when called on already initialized classes" do
    lambda{
      Fixnum.send :initialize
    }.should raise_error(TypeError)

    lambda{
      Object.send :initialize
    }.should raise_error(TypeError)
  end

  ruby_version_is "1.9" do
    # See [redmine:2601]
    it "raises a TypeError when called on BasicObject" do
      lambda{
        BasicObject.send :initialize
      }.should raise_error(TypeError)
    end
  end
end
