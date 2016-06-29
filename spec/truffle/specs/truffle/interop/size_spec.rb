# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.size" do
  
  class InteropSizeClass
    
    def size
      14
    end
    
  end
  
  it "returns the size of an array" do
    Truffle::Interop.size([1, 2, 3]).should == 3
  end
  
  it "returns the size of an array" do
    Truffle::Interop.size({a: 1, b: 2, c: 3}).should == 3
  end
  
  it "returns the size of an string" do
    Truffle::Interop.size('123').should == 3
  end
  
  it "returns the size of any object with a size method" do
    Truffle::Interop.size(InteropSizeClass.new).should == 14
  end

end
