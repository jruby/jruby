require File.expand_path('../../../../spec_helper', __FILE__)
require 'stringio'
require 'zlib'

describe "GzipReader#getc" do

  before :each do
    @data = '12345abcde'
    @zip = "\037\213\b\000,\334\321G\000\00334261MLJNI\005\000\235\005\000$\n\000\000\000"
    @io = StringIO.new @zip
  end

  it "returns the next character from the stream" do
    gz = Zlib::GzipReader.new @io
    gz.pos.should == 0

    gz.getc.should == '1'
    gz.getc.should == '2'
    gz.getc.should == '3'
    gz.getc.should == '4'
    gz.getc.should == '5'
  end

  it "increments position" do
    gz = Zlib::GzipReader.new @io
    (0..@data.size).each do |i|
      gz.pos.should == i
      gz.getc
    end
  end

  it "returns nil at the end of the stream" do
    gz = Zlib::GzipReader.new @io
    gz.read
    pos = gz.pos
    gz.getc.should be_nil
    gz.pos.should == pos
  end

end
