# -*- encoding: us-ascii -*-
require 'zlib'
require File.expand_path('../../../../spec_helper', __FILE__)

describe "Zlib::Deflate.deflate" do
  it "deflates some data" do
    data = "\000" * 10

    zipped = Zlib::Deflate.deflate data

    zipped.should == "x\234c`\200\001\000\000\n\000\001"
  end

  it "deflates lots of data" do
    data = "\000" * 32 * 1024

    zipped = Zlib::Deflate.deflate data

    zipped.should == "x\234\355\301\001\001\000\000\000\200\220\376\257\356\b\n#{"\000" * 31}\030\200\000\000\001"
  end

  it "deflates chunked data" do
    random_generator = Random.new(0)
    deflated         = ''

    Zlib.deflate(random_generator.bytes(20000)) do |chunk|
      deflated << chunk
    end

    deflated.length.should == 20016
  end
end

describe "Zlib::Deflate#deflate" do
  before :each do
    @deflator = Zlib::Deflate.new
  end

  it "deflates some data" do
    data = "\000" * 10

    zipped = @deflator.deflate data, Zlib::FINISH
    @deflator.finish

    zipped.should == "x\234c`\200\001\000\000\n\000\001"
  end

  it "deflates lots of data" do
    data = "\000" * 32 * 1024

    zipped = @deflator.deflate data, Zlib::FINISH
    @deflator.finish

    zipped.should == "x\234\355\301\001\001\000\000\000\200\220\376\257\356\b\n#{"\000" * 31}\030\200\000\000\001"
  end
end

describe "Zlib::Deflate#deflate" do

  before :each do
    @deflator         = Zlib::Deflate.new
    @random_generator = Random.new(0)
    @original         = ''
    @chunks           = []
  end

  describe "without break" do

    before do
      2.times do
        @input = @random_generator.bytes(20000)
        @original << @input
        @deflator.deflate(@input) do |chunk|
          @chunks << chunk
        end
      end
    end

    it "deflates chunked data" do
      @deflator.finish
      @chunks.map { |chunk| chunk.length }.should == [16384, 16384]
    end

    it "deflates chunked data with final chunk" do
      final = @deflator.finish
      final.length.should == 7253
    end

    it "deflates chunked data without errors" do
      final = @deflator.finish
      @chunks << final
      @original.should == Zlib.inflate(@chunks.join)
    end

  end

  describe "with break" do
    before :each do
      @input = @random_generator.bytes(20000)
      @deflator.deflate(@input) do |chunk|
        @chunks << chunk
        break
      end
    end

    it "deflates only first chunk" do
      @deflator.finish
      @chunks.map { |chunk| chunk.length }.should == [16384]
    end

    it "deflates chunked data with final chunk" do
      final = @deflator.finish
      final.length.should == 3632
    end

    it "deflates chunked data without errors" do
      final = @deflator.finish
      @chunks << final
      @input.should == Zlib.inflate(@chunks.join)
    end

  end
end
