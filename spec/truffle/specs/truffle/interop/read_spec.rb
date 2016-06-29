# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.read" do
  
  class ReadInstanceVariable
    
    def initialize
      @foo = 14
    end
    
  end
  
  class HasMethod
    
    def foo
      14
    end
    
  end
  
  class HasIndex
    
    def [](n)
      14
    end
    
  end

  it "reads a byte from a string" do
    Truffle::Interop.read('123', 1).should == '2'.ord
  end
  
  describe "reads an instance variable if given an @name" do
    it "as a symbol" do
      Truffle::Interop.read(ReadInstanceVariable.new, :@foo).should == 14
    end
    
    it "as a string" do
      Truffle::Interop.read(ReadInstanceVariable.new, '@foo').should == 14
    end
  end
  
  describe "calls #[] if there isn't a method with the same name" do
    it "as a symbol" do
      Truffle::Interop.read(HasIndex.new, :foo).should == 14
    end
    
    it "as a string" do
      Truffle::Interop.read(HasIndex.new, 'foo').should == 14
    end
  end
  
  describe "calls a method if there is a method with the same name" do
    it "as a symbol" do
      Truffle::Interop.read(HasMethod.new, :foo).should == 14
    end
    
    it "as a string" do
      Truffle::Interop.read(HasMethod.new, 'foo').should == 14
    end
  end

  it "can be used to index an array" do
    Truffle::Interop.read([1, 2, 3], 1).should == 2
  end

  it "can be used to index a hash" do
    Truffle::Interop.read({1 => 2, 3 => 4, 5 => 6}, 3).should == 4
  end

end
