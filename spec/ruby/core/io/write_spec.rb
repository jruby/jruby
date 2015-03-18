# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/write', __FILE__)
require File.expand_path('../shared/binwrite', __FILE__)

describe "IO#write on a file" do
  before :each do
    @filename = tmp("IO_syswrite_file") + $$.to_s
    File.open(@filename, "w") do |file|
      file.write("012345678901234567890123456789")
    end
    @file = File.open(@filename, "r+")
    @readonly_file = File.open(@filename)
  end

  after :each do
    @file.close
    @readonly_file.close
    rm_r @filename
  end

  # TODO: impl detail? discuss this with matz. This spec is useless. - rdavis
  # I agree. I've marked it not compliant on macruby, as we don't buffer input. -pthomson
  not_compliant_on :macruby do
    it "writes all of the string's bytes but buffers them" do
      written = @file.write("abcde")
      written.should == 5
      File.open(@filename) do |file|
        file.read.should == "012345678901234567890123456789"
        @file.fsync
        file.rewind
        file.read.should == "abcde5678901234567890123456789"
      end
    end
  end

  it "does not check if the file is writable if writing zero bytes" do
    lambda { @readonly_file.write("") }.should_not raise_error
  end

  it "returns a length of 0 when writing a blank string" do
    @file.write('').should == 0
  end

  with_feature :encoding do
    before :each do
      @external = Encoding.default_external
      @internal = Encoding.default_internal

      Encoding.default_external = Encoding::UTF_8
    end

    after :each do
      Encoding.default_external = @external
      Encoding.default_internal = @internal
    end

    it "returns the number of bytes written" do
      @file.write("hellø").should == 6
    end

    it "uses the encoding from the given option for non-ascii encoding" do
      File.open(@filename, "w", :encoding => Encoding::UTF_32LE) do |file|
        file.write("hi").should == 8
      end
      File.binread(@filename).should == "h\u0000\u0000\u0000i\u0000\u0000\u0000"
    end

    it "uses an :open_args option" do
      IO.write(@filename, 'hi', :open_args => ["w", nil, {:encoding => Encoding::UTF_32LE}]).should == 8
    end

    it "raises a invalid byte sequence error if invalid bytes are being written" do
      File.open(@filename, "w", :encoding => Encoding::US_ASCII) do |file|
        lambda { file.write("\xFEhi") }.should raise_error(Encoding::InvalidByteSequenceError)
      end
    end

    it "writes binary data if no encoding is given" do
      File.open(@filename, "w") do |file|
        file.write('Hëllö'.encode('ISO-8859-1'))
      end
      File.binread(@filename).should == "H\xEBll\xF6".force_encoding(Encoding::ASCII_8BIT)
    end
  end
end

describe "IO.write" do
  it_behaves_like :io_binwrite, :write

  it "uses an :open_args option" do
    IO.write(@filename, 'hi', :open_args => ["w", nil, {:encoding => Encoding::UTF_32LE}]).should == 8
  end

  it "disregards other options if :open_args is given" do
    IO.write(@filename, 'hi', 2, :mode => "r", :encoding => Encoding::UTF_32LE, :open_args => ["w"]).should == 2
    File.read(@filename).should == "\0\0hi"
  end

  it "uses the given encoding and returns the number of bytes written" do
    IO.write(@filename, 'hi', :mode => "w", :encoding => Encoding::UTF_32LE).should == 8
  end

  it "writes binary data if no encoding is given" do
    IO.write(@filename, 'Hëllö'.encode('ISO-8859-1'))
    File.binread(@filename).should == "H\xEBll\xF6".force_encoding(Encoding::ASCII_8BIT)
  end

  platform_is_not :windows do
    describe "on a FIFO" do
      before :each do
        @fifo = tmp("File_open_fifo")
        system "mkfifo #{@fifo}"
      end

      after :each do
        rm_r @fifo
      end

      it "writes correctly" do
        thr = Thread.new do
          sleep 0.5
          IO.read(@fifo)
        end
        string = "hi"
        IO.write(@fifo, string).should == string.length
      end
    end
  end
end

describe "IO#write" do
  it_behaves_like :io_write, :write
end

platform_is :windows do
  describe "IO#write on Windows" do
    before :each do
      @fname = tmp("io_write.txt")
    end

    after :each do
      rm_r @fname
      @io.close if @io and !@io.closed?
    end

    it "normalizes line endings in text mode" do
      @io = new_io(@fname, "w")
      @io.write "a\r\nb\r\nc"
      @io.close
      @fname.should have_data("a\nb\nc")
    end

    it "does not normalize line endings in binary mode" do
      @io = new_io(@fname, "wb")
      @io.write "a\r\nb\r\nc"
      @io.close
      @fname.should have_data("a\r\nb\r\nc")
    end
  end
end
