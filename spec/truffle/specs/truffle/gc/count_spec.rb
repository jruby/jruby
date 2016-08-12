# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::GC.count" do
  
  it "returns an Integer" do
    Truffle::GC.count.should be_kind_of(Integer)
  end
  
  it "increases as collections are run" do
    count_before = Truffle::GC.count
    i = 0
    while Truffle::GC.count <= count_before and i < 10
      GC.start
      i += 1
    end
    Truffle::GC.count.should > count_before
  end

end
