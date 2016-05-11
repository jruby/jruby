require File.expand_path('../../../spec_helper', __FILE__)

describe "Array#max" do
  describe "given a block with one argument" do
    it "yields in turn the last length-1 values from the array" do
      ary = []
      result = [1,2,3,4,5].max {|x| ary << x; x}

      ary.should == [2,3,4,5]
      result.should == 5
    end
  end
end