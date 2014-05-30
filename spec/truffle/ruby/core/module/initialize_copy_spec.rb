require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#initialize_copy" do

  ruby_bug "#7557", "1.9.3.327" do

    it "raises a TypeError when called on already initialized classes" do
      lambda{
        m = Module.new
        String.send :initialize_copy, m
      }.should raise_error(TypeError)
    end

  end

end
