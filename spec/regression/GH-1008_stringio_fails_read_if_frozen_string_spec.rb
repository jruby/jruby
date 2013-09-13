require 'rspec'
require 'stringio'

describe "StringIO#read" do
  it "works when the contained string is frozen" do
    str = "Hello".freeze
    strio = StringIO.new(str)

    strio.read.should == str
  end
end
