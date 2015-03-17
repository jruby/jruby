require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.file" do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  # NOTE: this test assumes that fixtures files have two lines each
  it "returns the current file object on each file" do
    argv [@file1, @file2] do
      result = []
      # returns first current file even when not yet open
      result << ARGF.file.path
      result << ARGF.file.path while ARGF.gets
      # returns last current file even when closed
      result << ARGF.file.path
      result.should == [@file1, @file1, @file1, @file2, @file2, @file2]
    end
  end
end
