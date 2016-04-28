# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Fixnum#&" do
  before :each do
    @long = (1 << 48) + 1
    @mask = Truffle::Fixnum.lower((1 << 30) - 1)
  end

  it "returns an int for (int, int)" do
    result = (1 & 3)
    result.should == 1
    Truffle.java_class_of(result).should == 'Integer'
  end

  it "returns an int for (long, int)" do
    Truffle.java_class_of(@long).should == 'Long'
    Truffle.java_class_of(@mask).should == 'Integer'

    result = (@long & @mask)
    result.should == 1
    Truffle.java_class_of(result).should == 'Integer'
  end

  it "returns an int for (int, long)" do
    Truffle.java_class_of(@long).should == 'Long'
    Truffle.java_class_of(@mask).should == 'Integer'

    result = (@mask & @long)
    result.should == 1
    Truffle.java_class_of(result).should == 'Integer'
  end
end
