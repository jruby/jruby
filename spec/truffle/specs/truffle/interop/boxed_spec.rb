# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.boxed?" do
  
  it "returns false for empty strings" do
    Truffle::Interop.boxed?('').should be_false
  end
    
  it "returns true for strings with one byte" do
    Truffle::Interop.boxed?('1').should be_true
  end
    
  it "returns false for strings with two bytes" do
    Truffle::Interop.boxed?('12').should be_false
  end

end
