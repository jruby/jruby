require File.expand_path('../../../spec_helper', __FILE__)

describe "Array#dig" do

  it "returns at with one arg" do
    ['a'].dig(0).should == 'a'
    ['a'].dig(1).should be_nil
  end

  it "recurses array elements" do
    a = [ [ 1, [2, '3'] ] ]
    a.dig(0, 0).should == 1
    a.dig(0, 1, 1).should == '3'
    a.dig(0, -1, 0).should == 2
  end

  it "raises without any args" do
    lambda { [10].dig() }.should raise_error(ArgumentError)
  end

end