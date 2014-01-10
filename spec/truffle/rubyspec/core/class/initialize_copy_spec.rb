require File.expand_path('../../../spec_helper', __FILE__)

describe "Class#initialize_copy" do
  it "raises a TypeError when called on already initialized classes" do
    lambda{
      String.send :initialize_copy, Fixnum
    }.should raise_error(TypeError)

    lambda{
      Object.send :initialize_copy, String
    }.should raise_error(TypeError)
  end

  ruby_version_is "1.9" do
    # See [redmine:2601]
    it "raises a TypeError when called on BasicObject" do
      lambda{
        BasicObject.send :initialize_copy, String
      }.should raise_error(TypeError)
    end
  end
end
