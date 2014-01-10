require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.to_s" do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "returns 'ARGF'" do
    argv [@file1, @file2] do
      ARGF.to_s.should == "ARGF"
    end
  end
end
