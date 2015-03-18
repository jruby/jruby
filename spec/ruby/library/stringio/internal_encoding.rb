require 'stringio'
require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9.2" do
  describe "StringIO#internal_encoding" do
    it "returns nil" do
      io = StringIO.new
      io.set_encoding Encoding::UTF8
      io.internal_encoding.should == nil
    end
  end
end
