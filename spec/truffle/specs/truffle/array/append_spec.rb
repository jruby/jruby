# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe "Array#<<" do
  def storage(ary)
    Truffle::Debug.array_storage(ary)
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
