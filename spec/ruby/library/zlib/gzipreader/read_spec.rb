require File.expand_path('../../../../spec_helper', __FILE__)
require 'stringio'
require 'zlib'

describe "GzipReader#read" do

  before :each do
    @data = '12345abcde'
    @zip = "\037\213\b\000,\334\321G\000\00334261MLJNI\005\000\235\005\000$\n\000\000\000"
    @io = StringIO.new @zip
  end

  it "with no arguments reads the entire content of a gzip file" do
    gz = Zlib::GzipReader.new @io
    gz.read.should == @data
  end

  it "with nil length argument reads the entire content of a gzip file" do
    gz = Zlib::GzipReader.new @io
    gz.read(nil).should == @data
  end

  it "reads the contents up to a certain size" do
    gz = Zlib::GzipReader.new @io
    gz.read(5).should == @data[0...5]
    gz.read(5).should == @data[5...10]
  end

  it "does not accept a negative length to read" do
    gz = Zlib::GzipReader.new @io
    lambda {
      gz.read(-1)
    }.should raise_error(ArgumentError)
  end

  it "returns an empty string if a 0 length is given" do
    gz = Zlib::GzipReader.new @io
    gz.read(0).should == ""
  end

  ruby_version_is "1.9" do
    it "respects :external_encoding option" do
      gz = Zlib::GzipReader.new(@io, :external_encoding => 'UTF-8')
      gz.read.encoding.should == Encoding::UTF_8

      @io.rewind
      gz = Zlib::GzipReader.new(@io, :external_encoding => 'UTF-16LE')
      gz.read.encoding.should == Encoding::UTF_16LE
    end
  end

  describe "at the end of data" do
    it "returns empty string if length prameter is not specified or 0" do
      gz = Zlib::GzipReader.new @io
      gz.read # read till the end
      gz.read(0).should == ""
      gz.read().should == ""
      gz.read(nil).should == ""
    end

    it "returns nil if length prameter is positive" do
      gz = Zlib::GzipReader.new @io
      gz.read # read till the end
      gz.read(1).should be_nil
      gz.read(2**16).should be_nil
    end
  end

end
