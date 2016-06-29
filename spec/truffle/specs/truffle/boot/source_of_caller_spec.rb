# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Boot.source_of_caller" do

  def source_of_caller
    Truffle::Boot.source_of_caller
  end

  #it "returns nil if there is no caller"
  #end
  
  it "returns a String" do
    source_of_caller.should be_kind_of(String)
  end
  
  it "returns the name of the file at the call site" do
    source_of_caller.should == __FILE__
  end
  
  it "works through #send" do
    x = 14
    Truffle::Boot.send(:source_of_caller).should == __FILE__
  end

end
