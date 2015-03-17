# -*- encoding: us-ascii -*-
require 'zlib'
require File.expand_path('../../../../spec_helper', __FILE__)

describe "Zlib::Inflate#inflate" do

  before :each do
    @inflator = Zlib::Inflate.new
  end

  it "inflates some data" do
    data = "x\234c`\200\001\000\000\n\000\001"

    unzipped = @inflator.inflate data
    @inflator.finish

    unzipped.should == "\000" * 10
  end

  it "inflates lots of data" do
    data = "x\234\355\301\001\001\000\000\000\200\220\376\257\356\b\n#{"\000" * 31}\030\200\000\000\001"

    unzipped = @inflator.inflate data
    @inflator.finish

    unzipped.should == "\000" * 32 * 1024
  end

  it "works in pass-through mode, once finished" do
    data = "x\234c`\200\001\000\000\n\000\001"

    @inflator.inflate data
    @inflator.finish  # this is a precondition

    out = @inflator.inflate('uncompressed_data')
    out << @inflator.finish
    out.should == 'uncompressed_data'

    @inflator << ('uncompressed_data') << nil
    @inflator.finish.should == 'uncompressed_data'
  end

end

describe "Zlib::Inflate::inflate" do

  it "inflates some data" do
    data = "x\234c`\200\001\000\000\n\000\001"

    unzipped = Zlib::Inflate.inflate data

    unzipped.should == "\000" * 10
  end

  it "inflates lots of data" do
    data = "x\234\355\301\001\001\000\000\000\200\220\376\257\356\b\n#{"\000" * 31}\030\200\000\000\001"

    zipped = Zlib::Inflate.inflate data

    zipped.should == "\000" * 32 * 1024
  end

  it "properly handles data in chunks" do
    data =  "x\234K\313\317\a\000\002\202\001E"
    z = Zlib::Inflate.new
    # add bytes, one by one
    result = ""
    data.each_byte { |d| result << z.inflate(d.chr)}
    result << z.finish
    result.should == "foo"
  end

  it "properly handles incomplete data" do
    data =  "x\234K\313\317\a\000\002\202\001E"[0,5]
    z = Zlib::Inflate.new
    # add bytes, one by one, but not all
    result = ""
    data.each_byte { |d| result << z.inflate(d.chr)}
    lambda { result << z.finish }.should raise_error(Zlib::BufError)
  end

  it "properly handles excessive data, byte-by-byte" do
    main_data = "x\234K\313\317\a\000\002\202\001E"
    data =  main_data * 2
    result = ""

    z = Zlib::Inflate.new
    # add bytes, one by one
    data.each_byte { |d| result << z.inflate(d.chr)}
    result << z.finish

    # the first chunk is inflated to its completion,
    # the second chunk is just passed through.
    result.should == "foo" + main_data
  end

  it "properly handles excessive data, in one go" do
    main_data = "x\234K\313\317\a\000\002\202\001E"
    data =  main_data * 2
    result = ""

    z = Zlib::Inflate.new
    result << z.inflate(data)
    result << z.finish

    # the first chunk is inflated to its completion,
    # the second chunk is just passed through.
    result.should == "foo" + main_data
  end
end
