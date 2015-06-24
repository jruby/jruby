# Copyright (c) 2008 Engine Yard, Inc. All rights reserved.

# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation
# files (the "Software"), to deal in the Software without
# restriction, including without limitation the rights to use,
# copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following
# conditions:

# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.

# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
# OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

# From Enumerable#max, adapted to Array since we have Array#max.
describe "Array#max" do
  before :each do
    @a = [2, 4, 6, 8, 10]

    @e_strs = ["333", "22", "666666", "1", "55555", "1010101010"]
    @e_ints = [333,   22,   666666,   55555, 1010101010]
  end

  it "max should return the maximum element" do
    [18, 42].max.should == 42
    [2, 5, 3, 6, 1, 4].max.should == 6
  end

  it "returns the maximum element (basics cases)" do
    [55].max.should == 55

    [11,99].max.should == 99
    [99,11].max.should == 99
    [2, 33, 4, 11].max.should == 33

    [1,2,3,4,5].max.should == 5
    [5,4,3,2,1].max.should == 5
    [1,4,3,5,2].max.should == 5
    [5,5,5,5,5].max.should == 5

    ["aa","tt"].max.should == "tt"
    ["tt","aa"].max.should == "tt"
    ["2","33","4","11"].max.should == "4"

    @e_strs.max.should == "666666"
    @e_ints.max.should == 1010101010
  end

  it "returns nil for an empty Enumerable" do
    [].max.should == nil
  end

  it "raises a NoMethodError for elements without #<=>" do
    lambda do
      [BasicObject.new, BasicObject.new].max
    end.should raise_error(NoMethodError)
  end

  it "raises an ArgumentError for incomparable elements" do
    lambda do
      [11,"22"].max
    end.should raise_error(ArgumentError)
    lambda do
      [11,12,22,33].max{|a, b| nil}
    end.should raise_error(ArgumentError)
  end

  it "returns the maximum element (with block)" do
    # with a block
    ["2","33","4","11"].max {|a,b| a <=> b }.should == "4"
    [ 2 , 33 , 4 , 11 ].max {|a,b| a <=> b }.should == 33

    ["2","33","4","11"].max {|a,b| b <=> a }.should == "11"
    [ 2 , 33 , 4 , 11 ].max {|a,b| b <=> a }.should == 2

    @e_strs.max {|a,b| a.length <=> b.length }.should == "1010101010"

    @e_strs.max {|a,b| a <=> b }.should == "666666"
    @e_strs.max {|a,b| a.to_i <=> b.to_i }.should == "1010101010"

    @e_ints.max {|a,b| a <=> b }.should == 1010101010
    @e_ints.max {|a,b| a.to_s <=> b.to_s }.should == 666666
  end

  it "returns the minimum for enumerables that contain nils" do
    arr = [nil, nil, true]
    arr.max { |a, b|
      x = a.nil? ? 1 : a ? 0 : -1
      y = b.nil? ? 1 : b ? 0 : -1
      x <=> y
    }.should == nil
  end

  it "gathers whole arrays as elements when each yields multiple" do
    multi = [[1,2], [3,4,5], [6,7,8,9]]
    multi.max.should == [6, 7, 8, 9]
  end

end
