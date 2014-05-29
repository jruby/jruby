# -*- encoding: ascii-8bit -*-
require File.expand_path('../../../../spec_helper', __FILE__)
require 'stringio'
require 'zlib'

describe "GzipWriter#write" do
  before :each do
    @data = '12345abcde'
    @zip = "\037\213\b\000,\334\321G\000\00334261MLJNI\005\000\235\005\000$\n\000\000\000"
    @io = StringIO.new ""
  end

  it "writes some compressed data" do
    Zlib::GzipWriter.wrap @io do |gzio|
      gzio.write @data
    end

    # skip gzip header for now
    @io.string[10..-1].should == @zip[10..-1]
  end

  it "returns the number of bytes in the input" do
    Zlib::GzipWriter.wrap @io do |gzio|
      gzio.write(@data).should == @data.size
    end
  end

  it "handles inputs of 2^23 bytes" do
    input = '.' * (2 ** 23)

    Zlib::GzipWriter.wrap @io do |gzio|
      gzio.write input
    end

    @io.string.size.should == 8176
  end
end
