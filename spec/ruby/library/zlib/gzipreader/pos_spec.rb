require File.expand_path('../../../../spec_helper', __FILE__)
require 'stringio'
require 'zlib'

describe "GzipReader#pos" do

  before :each do
    @data = '12345abcde'
    @zip = "\037\213\b\000,\334\321G\000\00334261MLJNI\005\000\235\005\000$\n\000\000\000"
    @io = StringIO.new @zip
  end

  it "returns the position" do
    gz = Zlib::GzipReader.new @io

    gz.pos.should == 0

    gz.read 5
    gz.pos.should == 5

    gz.read
    gz.pos.should == @data.length
  end

end

