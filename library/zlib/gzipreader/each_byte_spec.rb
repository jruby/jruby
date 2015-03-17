require File.expand_path('../../../../spec_helper', __FILE__)
require 'stringio'
require 'zlib'

describe "GzipReader#each_byte" do

  before :each do
    @data = '12345abcde'
    @zip = "\037\213\b\000,\334\321G\000\00334261MLJNI\005\000\235\005\000$\n\000\000\000"
    @io = StringIO.new @zip
    ScratchPad.clear
  end

  it "calls the given block for each byte in the stream, passing the byte as an argument" do
    gz = Zlib::GzipReader.new @io

    ScratchPad.record []
    gz.each_byte { |b| ScratchPad << b }

    ScratchPad.recorded.should == [49, 50, 51, 52, 53, 97, 98, 99, 100, 101]
  end

  it "increments position before calling the block" do
    gz = Zlib::GzipReader.new @io

    i = 1
    gz.each_byte do |ignore|
      gz.pos.should == i
      i += 1
    end
  end

end
