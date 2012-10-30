require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require "stringio"

ruby_version_is ''...'1.9.3' do
  describe "Net::HTTPResponse#to_ary" do
    before(:each) do
      @res = Net::HTTPUnknownResponse.new("1.0", "???", "test response")

      socket = Net::BufferedIO.new(StringIO.new("test body"))
      @res.reading_body(socket, true) {}
    end

    it "returns an Array containing a duplicate of self and self's body" do
      ary = @res.to_ary
      ary.size.should == 2

      ary[0].inspect.should == @res.inspect
      ary[1].should == "test body"
    end

    it "removes #to_ary from the duplicate of self" do
      @res.to_ary[0].respond_to?(:to_ary).should be_false
    end
  end
end
