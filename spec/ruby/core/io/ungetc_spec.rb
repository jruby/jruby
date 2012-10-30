require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "1.9" do
  class IO
    alias getc_orig getc
    def getc
      s = getc_orig
      s ? s.ord : s
    end
  end
end

describe "IO#ungetc" do
  before :each do
    @io = IOSpecs.io_fixture "lines.txt"

    @empty = tmp('empty.txt')
  end

  after :each do
    @io.close unless @io.closed?
    rm_r @empty
  end

  it "pushes back one character onto stream" do
    @io.getc.should == 86
    @io.ungetc(86)
    @io.getc.should == 86

    @io.ungetc(10)
    @io.getc.should == 10

    @io.getc.should == 111
    @io.getc.should == 105
    # read the rest of line
    @io.readline.should == "ci la ligne une.\n"
    @io.getc.should == 81
    @io.ungetc(99)
    @io.getc.should == 99
  end

  it "pushes back one character when invoked at the end of the stream" do
    # read entire content
    @io.read
    @io.ungetc(100)
    @io.getc.should == 100
  end

  it "pushes back one character when invoked at the start of the stream" do
    @io.read(0)
    @io.ungetc(100)
    @io.getc.should == 100
  end

  it "pushes back one character when invoked on empty stream" do
    touch(@empty)

    File.open(@empty) { |empty|
      empty.getc().should == nil
      empty.ungetc(10)
      empty.getc.should == 10
    }
  end

  it "affects EOF state" do
    touch(@empty)

    File.open(@empty) { |empty|
      empty.eof?.should == true
      empty.getc.should == nil
      empty.ungetc(100)
      empty.eof?.should == false
    }
  end

  it "adjusts the stream position" do
    @io.pos.should == 0

    # read one char
    c = @io.getc
    @io.pos.should == 1
    @io.ungetc(c)
    @io.pos.should == 0

    # read all
    @io.read
    pos = @io.pos
    @io.ungetc(98)
    @io.pos.should == pos - 1
  end

  # TODO: file MRI bug
  # Another specified behavior that MRI doesn't follow:
  # "Has no effect with unbuffered reads (such as IO#sysread)."
  #
  #it "has no effect with unbuffered reads" do
  #  length = File.size(@io_name)
  #  content = @io.sysread(length)
  #  @io.rewind
  #  @io.ungetc(100)
  #  @io.sysread(length).should == content
  #end

  it "makes subsequent unbuffered operations to raise IOError" do
    @io.getc
    @io.ungetc(100)
    lambda { @io.sysread(1) }.should raise_error(IOError)
  end

  ruby_version_is "" ... "1.9" do
    it "raises IOError when invoked on stream that was not yet read" do
      lambda { @io.ungetc(100) }.should raise_error(IOError)
    end
  end

  ruby_version_is "1.9" do
    it "returns nil when invoked on stream that was not yet read" do
      @io.ungetc(100).should be_nil
    end
  end

  it "raises IOError on closed stream" do
    @io.getc
    @io.close
    lambda { @io.ungetc(100) }.should raise_error(IOError)
  end
end
