# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::GC.time" do
  
  it "returns an Integer" do
    Truffle::GC.time.should be_kind_of(Integer)
  end
  
  it "increases as collections are run" do
    time_before = Truffle::GC.time
    escape = []
    100_000.times do
      escape << Time.now.to_s
    end
    Truffle::GC.time.should > time_before
  end

end
