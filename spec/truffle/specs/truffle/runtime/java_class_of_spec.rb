# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Runtime.java_class_of" do

  it "returns a String" do
    Truffle::Runtime.java_class_of(14).should be_kind_of(String)
  end

  it "returns 'Boolean' for true" do
    Truffle::Runtime.java_class_of(true).should == 'Boolean'
  end

  it "returns 'Boolean' for false" do
    Truffle::Runtime.java_class_of(false).should == 'Boolean'
  end

  it "returns 'Integer' for a small Fixnum" do
    Truffle::Runtime.java_class_of(14).should == 'Integer'
  end

  it "returns 'Long' for a large Fixnum" do
    Truffle::Runtime.java_class_of(0xffffffffffff).should == 'Long'
  end

  it "returns 'Double' for a Float" do
    Truffle::Runtime.java_class_of(3.14).should == 'Double'
  end

  it "returns 'RubyString' for a String" do
    Truffle::Runtime.java_class_of('test').should == 'RubyString'
  end

end
