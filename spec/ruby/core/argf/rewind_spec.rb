require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.rewind" do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @file1 = File.readlines @file1_name
    @file2 = File.readlines @file2_name
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  # NOTE: this test assumes that fixtures files have two lines each
  it "goes back to beginning of current file" do
    argv [@file1_name, @file2_name] do
      ARGF.gets;
      ARGF.rewind;
      ARGF.gets.should == @file1.first

      ARGF.gets # finish reading file1

      ARGF.gets
      ARGF.rewind
      ARGF.gets.should == @file2.first
    end
  end

  it "resets ARGF.lineno to 0" do
    argv [@file2_name] do
      ARGF.lineno = 0
      ARGF.gets;
      ARGF.lineno.should > 0
      ARGF.rewind;
      ARGF.lineno.should == 0
    end
  end

  it "raises an ArgumentError when end of stream reached" do
    argv [@file1_name, @file2_name] do
      ARGF.read
      lambda { ARGF.rewind }.should raise_error(ArgumentError)
    end
  end
end
