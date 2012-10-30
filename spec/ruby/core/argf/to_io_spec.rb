require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.to_io" do
  before :each do
    @file1= fixture __FILE__, "file1.txt"
    @file2= fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  # NOTE: this test assumes that fixtures files have two lines each
  it "returns the IO of the current file" do
    argv [@file1, @file2] do
      result = []
      4.times do
        ARGF.gets
        result << ARGF.to_io
      end

      result.each { |io| io.should be_kind_of(IO) }
      result[0].should == result[1]
      result[2].should == result[3]
    end
  end
end
