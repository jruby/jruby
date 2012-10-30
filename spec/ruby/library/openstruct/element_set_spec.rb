require File.expand_path('../../../spec_helper', __FILE__)
require "ostruct"

describe "OpenStruct#[]=" do
  before :each do
    @os = OpenStruct.new
  end

  ruby_bug "redmine:4179", "1.9.2" do
    it "raises a NoMethodError" do
      lambda { @os[1] = 2 }.should raise_error(NoMethodError)
    end
  end
end
