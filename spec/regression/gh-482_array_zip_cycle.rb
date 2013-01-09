require 'rspec'

describe "zip an array with" do
  it "infinite enum returns a correct new array" do
    [1,2].zip([0].cycle).should == [[1,0], [2,0]]
  end

  it "infinite enum, appending block is yielded with correct argument" do
    arr = []
    [1,2].zip([0].cycle){|a| arr << a}
    arr.should == [[1,0], [2,0]]
  end

  it "another array returns a correct new array" do
    [1,2].zip([0]).should == [[1,0], [2,nil]]
  end

  it "another array, appending block is yielded with correct argument" do
    arr = []
    [1,2].zip([0]){|a| arr << a}
    arr.should == [[1,0], [2,nil]]
  end
end

