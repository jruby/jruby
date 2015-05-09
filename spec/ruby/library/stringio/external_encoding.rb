require 'stringio'
require File.expand_path('../../../spec_helper', __FILE__)

describe "StringIO#external_encoding" do
  it "gets the encoding of the underlying String" do
    io = StringIO.new
    io.set_encoding Encoding::UTF8
    io.external_encoding.should == Encoding::UTF8
  end
end
