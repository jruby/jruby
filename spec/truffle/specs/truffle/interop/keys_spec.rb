# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.keys" do
  
  class InteropKeysClass
    
    def initialize
      @a = 1
      @b = 2
      @c = 3
    end
    
  end
  
  it "returns the keys of a hash" do
    Truffle::Interop.keys({a: 1, b: 2, c: 3}).should == ['a', 'b', 'c']
  end
  
  it "returns the instance variables of something that isn't a hash" do
    Truffle::Interop.keys(InteropKeysClass.new).sort.should == ['a', 'b', 'c']
  end

end
