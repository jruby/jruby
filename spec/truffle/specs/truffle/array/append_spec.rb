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

describe "Array#<<" do
  def storage(ary)
    Truffle::Primitive.array_storage(ary)
  end

  before :each do
    @long = 1 << 52
    @long_ary = [@long, @long+1, @long+2]
  end

  it "empty array has null storage" do
    ary = []
    storage(ary).should == "null"
  end

  it "supports transitions from null storage" do
    ary = [] << 1
    ary.should == [1]
    storage(ary).should == "int[]"

    ary = [] << @long
    ary.should == [@long]
    storage(ary).should == "long[]"

    ary = [] << 1.33
    ary.should == [1.33]
    storage(ary).should == "double[]"

    o = Object.new
    ary = [] << o
    ary.should == [o]
    storage(ary).should == "Object[]"
  end

  it "supports adding the same type" do
    ary = [1] << 2
    ary.should == [1, 2]
    storage(ary).should == "int[]"

    ary = [@long] << @long+1
    ary.should == [@long, @long+1]
    storage(ary).should == "long[]"

    ary = [3.14] << 3.15
    ary.should == [3.14, 3.15]
    storage(ary).should == "double[]"

    a, b = Object.new, Object.new
    ary = [a] << b
    ary.should == [a, b]
    storage(ary).should == "Object[]"
  end

  it "supports long[] << int" do
    storage(@long_ary).should == "long[]"

    @long_ary << 1
    @long_ary.should == [@long, @long+1, @long+2, 1]
    storage(@long_ary).should == "long[]"
  end

  it "supports int[] << long and goes to long[]" do
    # Make sure long[] is chosen even if there was a int[] << Object before
    2.times do |i|
      setup = (i == 0)

      ary = [0, 1, 2]
      storage(ary).should == "int[]"

      obj = (i == 0) ? Object.new : @long
      ary << obj
      ary.should == [0, 1, 2, obj]
      storage(ary).should == (setup ? "Object[]" : "long[]")
    end
  end

  it "supports int[] << double" do
    ary = [0, 1, 2]
    storage(ary).should == "int[]"

    ary << 1.34
    ary.should == [0, 1, 2, 1.34]
    storage(ary).should == "Object[]"
  end

  it "supports long[] << double" do
    storage(@long_ary).should == "long[]"

    @long_ary << 1.34
    @long_ary.should == [@long, @long+1, @long+2, 1.34]
    storage(@long_ary).should == "Object[]"
  end

  it "supports double[] << int" do
    ary = [3.14, 3.15]
    storage(ary).should == "double[]"

    ary << 4
    ary.should == [3.14, 3.15, 4]
    storage(ary).should == "Object[]"
  end

  it "supports Object[] << int" do
    o = Object.new
    ary = [o]
    storage(ary).should == "Object[]"

    ary << 4
    ary.should == [o, 4]
    storage(ary).should == "Object[]"
  end
end
