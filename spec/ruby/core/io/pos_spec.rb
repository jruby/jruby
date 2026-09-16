require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative 'shared/pos'

describe "IO#pos" do
  it_behaves_like :io_pos, :pos
end

describe "IO#pos=" do
  it_behaves_like :io_set_pos, :pos=
end

platform_is :windows do
  describe "IO#pos on Windows" do
    before :each do
      @fname = tmp("io_pos.txt")
      touch(@fname, "wb") { |f| f.write "line one\r\nline two\r\n" }
    end

    after :each do
      @io.close if @io
      rm_r @fname
    end

    it "reports the byte offset in the file after reading a CRLF line in text mode" do
      @io = new_io(@fname, "r")
      @io.readline.should == "line one\n"
      @io.pos.should == 10
      @io.readline.should == "line two\n"
      @io.pos.should == 20
    end

    it "reports the byte size of the file after reading it in text mode" do
      @io = new_io(@fname, "r")
      @io.read.should == "line one\nline two\n"
      @io.pos.should == File.size(@fname)
    end

    it "reports the byte offset after reading a CRLF line in binary mode" do
      @io = new_io(@fname, "rb")
      @io.readline.should == "line one\r\n"
      @io.pos.should == 10
    end
  end
end
