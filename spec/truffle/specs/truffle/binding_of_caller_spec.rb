# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../ruby/spec_helper'

describe "Truffle.binding_of_caller" do

  def binding_of_caller
    Truffle.binding_of_caller
  end

  #it "returns nil if there is no caller"
  #end
  
  it "returns a Binding" do
    binding_of_caller.should be_kind_of(Binding)
  end
  
  it "gives read access to local variables at the call site" do
    x = 14
    binding_of_caller.local_variable_get(:x).should == 14
  end
  
  it "gives write access to local variables at the call site" do
    x = 2
    binding_of_caller.local_variable_set(:x, 14)
    x.should == 14
  end
  
  it "works through #send" do
    x = 14
    Truffle.send(:binding_of_caller).local_variable_get(:x).should == 14
  end

end
