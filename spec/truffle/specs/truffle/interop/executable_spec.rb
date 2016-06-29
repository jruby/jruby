# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.executable?" do
  
  def test_method
  end
  
  it "returns true for methods" do
    Truffle::Interop.executable?(method(:test_method)).should be_true
  end
    
  it "returns true for procs" do
    Truffle::Interop.executable?(proc { }).should be_true
  end
    
  it "returns true for lambdas" do
    Truffle::Interop.executable?(lambda { }).should be_true
  end
    
  it "returns false for nil" do
    Truffle::Interop.executable?(nil).should be_false
  end
    
  it "returns false for strings" do
    Truffle::Interop.executable?('hello').should be_false
  end

end
