# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../ruby/spec_helper'

describe "Truffle.source_of_caller" do
  
  it "returns a String" do
    Truffle.source_of_caller.should be_kind_of(String)
  end
  
  it "returns the name of the file at the call site" do
    Truffle.source_of_caller.should == __FILE__
  end
  
  it "works through #send" do
    x = 14
    Truffle.send(:source_of_caller).should == __FILE__
  end

end
