require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.seek" do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @file1 = File.readlines @file1_name
    @file2 = File.readlines @file2_name
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "sets the absolute position relative to beginning of file" do
    argv [@file1_name, @file2_name] do
      ARGF.seek 2
      ARGF.gets.should == @file1.first[2..-1]
      ARGF.seek @file1.first.size
      ARGF.gets.should == @file1.last
      ARGF.seek 0, IO::SEEK_END
      ARGF.gets.should == @file2.first
    end
  end

  it "sets the position relative to current position in file" do
    argv [@file1_name, @file2_name] do
      ARGF.seek 0, IO::SEEK_CUR
      ARGF.gets.should == @file1.first
      ARGF.seek -@file1.first.size+2, IO::SEEK_CUR
      ARGF.gets.should == @file1.first[2..-1]
      ARGF.seek 1, IO::SEEK_CUR
      ARGF.gets.should == @file1.last[1..-1]
      ARGF.seek 3, IO::SEEK_CUR
      ARGF.gets.should == @file2.first
      ARGF.seek @file1.last.size, IO::SEEK_CUR
      ARGF.gets.should == nil
    end
  end

  it "sets the absolute position relative to end of file" do
    argv [@file1_name, @file2_name] do
      ARGF.seek -@file1.first.size-@file1.last.size, IO::SEEK_END
      ARGF.gets.should == @file1.first
      ARGF.seek -6, IO::SEEK_END
      ARGF.gets.should == @file1.last[-6..-1]
      ARGF.seek -4, IO::SEEK_END
      ARGF.gets.should == @file1.last[4..-1]
      ARGF.gets.should == @file2.first
      ARGF.seek -6, IO::SEEK_END
      ARGF.gets.should == @file2.last[-6..-1]
    end
  end

  it "takes at least one argument (offset)" do
    lambda { ARGF.seek }.should raise_error(ArgumentError)
  end
end
