require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.lineno" do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  # NOTE: this test assumes that fixtures files have two lines each
  # TODO: break this into four specs
  it "returns the current line number on each file" do
    argv [@file1, @file2, @file1, @file2] do
      ARGF.lineno = 0
      ARGF.gets
      ARGF.lineno.should == 1
      ARGF.gets
      ARGF.lineno.should == 2
      ARGF.gets
      ARGF.lineno.should == 3
      ARGF.gets
      ARGF.lineno.should == 4
    end
  end

  it "resets to 0 after the stream is rewound" do
    argv [@file1, @file2, @file1, @file2] do
      ARGF.lineno = 0
      ARGF.lineno.should == 0
      ARGF.readline
      ARGF.rewind
      ARGF.lineno.should == 0
    end
  end

  it "aliases to $." do
    argv [@file1, @file2, @file1, @file2] do
      ARGF.gets
      $..should == 1
      ARGF.gets
      $..should == 2
      ARGF.gets
      $..should == 3
      ARGF.gets
      $..should == 4
    end
  end
end
