# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.write" do
  
  class HasMethod
    
    def foo=(value)
      @called = true
    end
    
    def called?
      @called
    end
    
  end
  
  class HasIndexSet
    
    def []=(n, value)
      @called = true
    end
    
    def called?
      @called
    end
    
  end

  describe "writes an instance variable if given an @name" do
    it "as a symbol" do
      object = Object.new
      Truffle::Interop.write ReadInstanceVariable.new, :@foo, 14
      object.instance_variable_get('foo').should == 14
    end
    
    it "as a string" do
      object = Object.new
      Truffle::Interop.write ReadInstanceVariable.new, '@foo', 14
      object.instance_variable_get('foo').should == 14
    end
  end
  
  describe "calls #[]= if there isn't a method with the same name" do
    it "as a symbol" do
      object = HasIndexSet.new
      Truffle::Interop.write object, :foo, 14
      object.called?.should be_true
    end
    
    it "as a string" do
      object = HasIndexSet.new
      Truffle::Interop.write object, 'foo', 14
      object.called?.should be_true
    end
  end
  
  describe "calls a method if there is a method with the same name plus =" do
    it "as a symbol" do
      object = HasMethod.new
      Truffle::Interop.write object, :foo, 14
      object.called?.should be_true
    end
    
    it "as a string" do
      object = HasMethod.new
      Truffle::Interop.write object, 'foo', 14
      object.called?.should be_true
    end
  end

  it "can be used to assign an array" do
    array = [1, 2, 3]
    Truffle::Interop.write array, 1, 14
    array[1].should == 14
  end

  it "can be used to assign a hash" do
    hash = {1 => 2, 3 => 4, 5 => 6}
    Truffle::Interop.write hash, 3, 14
    hash[3].should == 14
  end

end
