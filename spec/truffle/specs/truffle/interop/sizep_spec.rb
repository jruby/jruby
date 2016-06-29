# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.size?" do
  
  it "returns true for arrays" do
    Truffle::Interop.size?([]).should be_true
  end
  
  it "returns true for hashes" do
    Truffle::Interop.size?({}).should be_true
  end
  
  it "returns true for strings" do
    Truffle::Interop.size?('').should be_true
  end

  it "returns false for nil" do
    Truffle::Interop.size?(nil).should be_false
  end

end
