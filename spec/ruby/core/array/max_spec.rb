require File.expand_path('../../../spec_helper', __FILE__)

describe "Array#max" do
  it "returns nil with no values" do
    [].max.should == nil
  end

  it "returns only element in one element array" do
    [1].max.should == 1
  end

  it "returns largest value with multiple elements" do
    [1,2].max.should == 2
    [2,1].max.should == 2
  end
  
  describe "given a block with one argument" do
    it "yields in turn the last length-1 values from the array" do
      ary = []
      result = [1,2,3,4,5].max {|x| ary << x; x}

      ary.should == [2,3,4,5]
      result.should == 5
    end
  end
end
