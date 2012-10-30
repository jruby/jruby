require File.expand_path('../../../spec_helper', __FILE__)
require "ostruct"

describe "OpenStruct#[]" do
  before :each do
    @os = OpenStruct.new
  end

  it "raises a NoMethodError" do
    lambda { @os[1] }.should raise_error(NoMethodError)
  end
end
