require_relative '../../spec_helper'

describe "ARGF.readlines" do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"

    @lines  = File.readlines(@file1)
    @lines += File.readlines(@file2)
  end

  it "reads all lines of all files" do
    argf [@file1, @file2] do
      @argf.readlines.should == @lines
    end
  end

  it "returns an empty Array when end of stream reached" do
    argf [@file1, @file2] do
      @argf.read
      @argf.readlines.should == []
    end
  end
end

describe "ARGF.readlines when reading $stdin and $stdin is not an IO" do
  before :each do
    @stdin = $stdin
  end

  after :each do
    $stdin = @stdin
  end

  it "raises NoMethodError when $stdin has no public readlines" do
    $stdin = Object.new
    -> { ARGF.class.new.readlines }.should raise_error(NoMethodError)
  end
end
