# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.null?" do
  
  it "returns true for nil" do
    Truffle::Interop.null?(nil).should be_true
  end
    
  it "returns false for strings" do
    Truffle::Interop.null?('').should be_false
  end
    
  it "returns false for zero" do
    Truffle::Interop.null?(0).should be_false
  end
    
  it "returns false for false" do
    Truffle::Interop.null?(false).should be_false
  end

end
